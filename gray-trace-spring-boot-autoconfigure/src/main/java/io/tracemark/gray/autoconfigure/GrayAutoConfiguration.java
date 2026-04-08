package io.tracemark.gray.autoconfigure;

import io.tracemark.gray.autoconfigure.async.GrayExecutorBeanPostProcessor;
import io.tracemark.gray.autoconfigure.http.*;
import io.tracemark.gray.core.GrayProperties;
import org.springframework.boot.autoconfigure.condition.*;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.*;
import org.springframework.web.client.RestTemplate;

/**
 * 灰度追踪自动配置主入口
 *
 * <p>可选框架（Feign / RocketMQ / WebFlux）拆到各自独立配置类，
 * 通过类级 {@code @ConditionalOnClass} 保护，避免缺少依赖时 ClassNotFoundException。
 *
 * <pre>{@code
 * gray:
 *   trace:
 *     enabled: true
 *     mq.enabled: false   # 消息队列默认关闭
 * }</pre>
 */
@Configuration(proxyBeanMethods = false)
@ConditionalOnProperty(prefix = "gray.trace", name = "enabled", matchIfMissing = true)
@EnableConfigurationProperties
@Import({
        GrayFeignAutoConfiguration.class,
        GrayRocketMqAutoConfiguration.class,
        GrayWebFluxAutoConfiguration.class
})
public class GrayAutoConfiguration {

    // ─── 配置属性 ──────────────────────────────────────────────
    @Bean
    @ConfigurationProperties(prefix = "gray.trace")
    public GrayProperties grayProperties() {
        return new GrayProperties();
    }

    // ─── Servlet 入口（SB2: javax）────────────────────────────
    // SB3 的 jakarta filter 在 GrayJakartaServletFilterConfiguration 中注册

    /** Spring Boot 2.x - javax.servlet */
    @Bean
    @ConditionalOnClass(name = "javax.servlet.Filter")
    @ConditionalOnMissingClass("jakarta.servlet.Filter")
    @ConditionalOnProperty(prefix = "gray.trace.servlet", name = "enabled", matchIfMissing = true)
    public FilterRegistrationBean<io.tracemark.gray.autoconfigure.filter.GrayServletFilter>
    grayServletFilter() {
        FilterRegistrationBean<io.tracemark.gray.autoconfigure.filter.GrayServletFilter> bean =
                new FilterRegistrationBean<>(
                        new io.tracemark.gray.autoconfigure.filter.GrayServletFilter());
        bean.addUrlPatterns("/*");
        bean.setOrder(-100);
        bean.setName("grayServletFilter");
        return bean;
    }

    // ─── RestTemplate 出口 ────────────────────────────────────
    @Bean
    @ConditionalOnClass(RestTemplate.class)
    @ConditionalOnProperty(prefix = "gray.trace.rest-template", name = "enabled", matchIfMissing = true)
    public GrayRestTemplateBeanPostProcessor grayRestTemplateBeanPostProcessor(
            GrayProperties properties) {
        return new GrayRestTemplateBeanPostProcessor(properties);
    }

    // ─── OkHttp 出口 ──────────────────────────────────────────
    @Bean
    @ConditionalOnClass(name = "okhttp3.OkHttpClient")
    @ConditionalOnProperty(prefix = "gray.trace.ok-http", name = "enabled", matchIfMissing = true)
    public GrayOkHttpBeanPostProcessor grayOkHttpBeanPostProcessor(GrayProperties properties) {
        return new GrayOkHttpBeanPostProcessor(properties);
    }

    // ─── Apache HttpClient 4.x 出口 ──────────────────────────
    @Bean
    @ConditionalOnClass(name = "org.apache.http.impl.client.CloseableHttpClient")
    @ConditionalOnProperty(prefix = "gray.trace.apache-http-client", name = "enabled", matchIfMissing = true)
    public GrayApacheHttpClientBeanPostProcessor grayApacheHttpClientBeanPostProcessor(
            GrayProperties properties) {
        return new GrayApacheHttpClientBeanPostProcessor(properties);
    }

    // ─── Apache HttpClient 5.x 出口 ──────────────────────────
    @Bean
    @ConditionalOnClass(name = "org.apache.hc.client5.http.impl.classic.CloseableHttpClient")
    @ConditionalOnProperty(prefix = "gray.trace.apache-http-client", name = "enabled", matchIfMissing = true)
    public GrayApacheHttp5ClientBeanPostProcessor grayApacheHttp5ClientBeanPostProcessor(
            GrayProperties properties) {
        return new GrayApacheHttp5ClientBeanPostProcessor(properties);
    }

    // ─── 线程池上下文传递 ─────────────────────────────────────
    @Bean
    @ConditionalOnProperty(prefix = "gray.trace.thread-pool", name = "enabled", matchIfMissing = true)
    public GrayExecutorBeanPostProcessor grayExecutorBeanPostProcessor(GrayProperties properties) {
        return new GrayExecutorBeanPostProcessor(properties);
    }
}