package io.tracemark.gray.autoconfigure.http;

import io.tracemark.gray.core.GrayConstants;
import io.tracemark.gray.core.GrayContext;
import org.apache.http.HttpRequest;
import org.apache.http.HttpRequestInterceptor;
import org.apache.http.protocol.HttpContext;

/**
 * Apache HttpClient 4.x 出口灰度头传递拦截器
 *
 * <p>注册为 CloseableHttpClient 的请求拦截器，
 * 在请求发出前将当前灰度标签注入 Header。
 */
public class GrayApacheHttpClientInterceptor implements HttpRequestInterceptor {

    @Override
    public void process(HttpRequest request, HttpContext context) {
        String tag = GrayContext.get();
        if (tag == null || tag.isEmpty()) {
            return;
        }
        request.addHeader(GrayConstants.HEADER_GRAY_TAG, tag);
    }
}
