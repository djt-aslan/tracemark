package io.tracemark.gray.autoconfigure;

import io.tracemark.gray.autoconfigure.http.GrayFeignInterceptor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * OpenFeign 灰度配置（仅在 feign-core 存在时加载）
 *
 * <p>独立 @Configuration 类 + 类级 @ConditionalOnClass，确保 Feign 不存在时
 * Spring 不加载本类，避免 ClassNotFoundException。
 */
@Configuration(proxyBeanMethods = false)
@ConditionalOnClass(name = "feign.RequestInterceptor")
@ConditionalOnProperty(prefix = "gray.trace", name = "enabled", matchIfMissing = true)
public class GrayFeignAutoConfiguration {

    @Bean
    @ConditionalOnProperty(prefix = "gray.trace.feign", name = "enabled", matchIfMissing = true)
    public GrayFeignInterceptor grayFeignInterceptor() {
        return new GrayFeignInterceptor();
    }
}