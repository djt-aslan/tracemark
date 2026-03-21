package io.tracemark.gray.autoconfigure.http;

import io.tracemark.gray.core.GrayConstants;
import io.tracemark.gray.core.GrayContext;
import org.springframework.http.HttpRequest;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.ClientHttpResponse;

import java.io.IOException;

/**
 * RestTemplate 出口灰度头传递拦截器
 *
 * <p>在每次 RestTemplate 发出 HTTP 请求时，
 * 将当前线程的灰度标签注入请求 Header，传递给下游服务。
 */
public class GrayRestTemplateInterceptor implements ClientHttpRequestInterceptor {

    @Override
    public ClientHttpResponse intercept(HttpRequest request, byte[] body,
                                        ClientHttpRequestExecution execution) throws IOException {
        String tag = GrayContext.get();
        if (tag != null && !tag.isEmpty()) {
            request.getHeaders().set(GrayConstants.HEADER_GRAY_TAG, tag);
        }
        return execution.execute(request, body);
    }
}