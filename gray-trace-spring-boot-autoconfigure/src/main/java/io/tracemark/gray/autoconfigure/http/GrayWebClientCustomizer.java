package io.tracemark.gray.autoconfigure.http;

import org.springframework.boot.web.reactive.function.client.WebClientCustomizer;
import org.springframework.web.reactive.function.client.WebClient;

/**
 * 自动为 {@link WebClient.Builder} 注入灰度过滤器
 *
 * <p>Spring Boot 会自动发现所有 {@link WebClientCustomizer} Bean 并应用到 {@code WebClient.Builder}。
 */
public class GrayWebClientCustomizer implements WebClientCustomizer {

    @Override
    public void customize(WebClient.Builder webClientBuilder) {
        webClientBuilder.filter(GrayWebClientFilter.instance());
    }
}