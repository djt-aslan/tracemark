package io.tracemark.test.service;

import io.tracemark.gray.core.GrayConstants;
import io.tracemark.gray.core.GrayContext;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

/**
 * 验证 HTTP 出口场景的灰度头透传
 *
 * <p>调用本服务自身的 /gray/echo 接口，该接口会把收到的请求头原样回显，
 * 从而验证 x-gray-tag 是否被正确透传给下游。
 */
@Service
public class HttpClientService {

    private static final Logger log = LoggerFactory.getLogger(HttpClientService.class);

    // 下游目标：调用自身 /gray/echo 接口来回显 Header
    private static final String ECHO_URL = "http://localhost:8080/gray/echo";

    private final RestTemplate restTemplate;
    private final OkHttpClient okHttpClient;

    public HttpClientService(RestTemplate restTemplate, OkHttpClient okHttpClient) {
        this.restTemplate = restTemplate;
        this.okHttpClient = okHttpClient;
    }

    /**
     * 场景 5：RestTemplate 出口
     * Starter 自动注入了 GrayRestTemplateInterceptor，x-gray-tag 会被自动添加到请求头
     */
    public String callViaRestTemplate() {
        log.info("[HttpClientService] RestTemplate call, current grayTag={}", GrayContext.get());
        try {
            return restTemplate.getForObject(ECHO_URL, String.class);
        } catch (Exception e) {
            return "ERROR: " + e.getMessage() + " (start the server first)";
        }
    }

    /**
     * 场景 6：OkHttp 出口
     * Starter 自动注入了 GrayOkHttpInterceptor，x-gray-tag 会被自动添加到请求头
     */
    public String callViaOkHttp() {
        log.info("[HttpClientService] OkHttp call, current grayTag={}", GrayContext.get());
        Request request = new Request.Builder()
                .url(ECHO_URL)
                .build();
        try (Response response = okHttpClient.newCall(request).execute()) {
            return response.body() != null ? response.body().string() : "empty";
        } catch (Exception e) {
            return "ERROR: " + e.getMessage() + " (start the server first)";
        }
    }
}