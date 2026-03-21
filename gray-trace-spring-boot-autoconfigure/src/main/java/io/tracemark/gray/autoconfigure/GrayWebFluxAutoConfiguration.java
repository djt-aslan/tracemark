package io.tracemark.gray.autoconfigure;

import io.tracemark.gray.autoconfigure.http.GrayWebClientCustomizer;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;

/**
 * WebFlux / WebClient 灰度配置（仅在 spring-webflux 存在时加载）
 */
@Configuration(proxyBeanMethods = false)
@ConditionalOnClass(WebClient.class)
@ConditionalOnProperty(prefix = "gray.trace", name = "enabled", matchIfMissing = true)
public class GrayWebFluxAutoConfiguration {

    @Bean
    @ConditionalOnProperty(prefix = "gray.trace.web-flux", name = "enabled", matchIfMissing = true)
    public GrayWebClientCustomizer grayWebClientCustomizer() {
        return new GrayWebClientCustomizer();
    }
}