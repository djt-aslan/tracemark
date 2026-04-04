package io.tracemark.gray.autoconfigure.http;

import io.tracemark.gray.core.GrayConstants;
import io.tracemark.gray.core.GrayContext;
import org.apache.hc.core5.http.EntityDetails;
import org.apache.hc.core5.http.HttpRequest;
import org.apache.hc.core5.http.HttpRequestInterceptor;
import org.apache.hc.core5.http.protocol.HttpContext;

/**
 * Apache HttpClient 5.x 出口灰度头传递拦截器
 *
 * <p>注册为 CloseableHttpClient 的请求拦截器，
 * 在请求发出前将当前灰度标签注入 Header。
 */
public class GrayApacheHttp5ClientInterceptor implements HttpRequestInterceptor {

    @Override
    public void process(HttpRequest request, EntityDetails entity, HttpContext context) {
        String tag = GrayContext.get();
        if (tag == null || tag.isEmpty()) {
            return;
        }
        request.addHeader(GrayConstants.HEADER_GRAY_TAG, tag);
    }
}
