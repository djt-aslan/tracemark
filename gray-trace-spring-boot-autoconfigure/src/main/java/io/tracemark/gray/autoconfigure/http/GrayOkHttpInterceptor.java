package io.tracemark.gray.autoconfigure.http;

import io.tracemark.gray.core.GrayConstants;
import io.tracemark.gray.core.GrayContext;
import okhttp3.Interceptor;
import okhttp3.Request;
import okhttp3.Response;

import java.io.IOException;

/**
 * OkHttp 出口灰度头传递拦截器
 *
 * <p>注册为 OkHttpClient 的 Application Interceptor，
 * 在请求发出前将当前灰度标签追加到 Header。
 */
public class GrayOkHttpInterceptor implements Interceptor {

    @Override
    public Response intercept(Chain chain) throws IOException {
        String tag = GrayContext.get();
        Request original = chain.request();

        if (tag == null || tag.isEmpty()) {
            return chain.proceed(original);
        }

        Request newRequest = original.newBuilder()
                .header(GrayConstants.HEADER_GRAY_TAG, tag)
                .build();
        return chain.proceed(newRequest);
    }
}