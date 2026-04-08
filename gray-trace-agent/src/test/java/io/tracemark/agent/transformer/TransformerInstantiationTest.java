package io.tracemark.agent.transformer;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Transformer 类实例化测试
 * 验证 Transformer 可以正常创建
 */
class TransformerInstantiationTest {

    @Test
    @DisplayName("ThreadPoolTransformer 应能正常实例化")
    void threadPoolTransformer_shouldInstantiate() {
        ThreadPoolTransformer transformer = new ThreadPoolTransformer();
        assertNotNull(transformer);
    }

    @Test
    @DisplayName("CompletableFutureAsyncTransformer 应能正常实例化")
    void completableFutureAsyncTransformer_shouldInstantiate() {
        CompletableFutureAsyncTransformer transformer = new CompletableFutureAsyncTransformer();
        assertNotNull(transformer);
    }

    @Test
    @DisplayName("ServletInboundTransformer 应能正常实例化")
    void servletInboundTransformer_shouldInstantiate() {
        ServletInboundTransformer transformer = new ServletInboundTransformer();
        assertNotNull(transformer);
    }

    @Test
    @DisplayName("JakartaServletInboundTransformer 应能正常实例化")
    void jakartaServletInboundTransformer_shouldInstantiate() {
        JakartaServletInboundTransformer transformer = new JakartaServletInboundTransformer();
        assertNotNull(transformer);
    }

    @Test
    @DisplayName("OkHttpOutboundTransformer 应能正常实例化")
    void okHttpOutboundTransformer_shouldInstantiate() {
        OkHttpOutboundTransformer transformer = new OkHttpOutboundTransformer();
        assertNotNull(transformer);
    }

    @Test
    @DisplayName("RestTemplateOutboundTransformer 应能正常实例化")
    void restTemplateOutboundTransformer_shouldInstantiate() {
        RestTemplateOutboundTransformer transformer = new RestTemplateOutboundTransformer();
        assertNotNull(transformer);
    }

    @Test
    @DisplayName("ApacheHttpClientOutboundTransformer 应能正常实例化")
    void apacheHttpClientOutboundTransformer_shouldInstantiate() {
        ApacheHttpClientOutboundTransformer transformer = new ApacheHttpClientOutboundTransformer();
        assertNotNull(transformer);
    }

    @Test
    @DisplayName("ApacheHttp5ClientOutboundTransformer 应能正常实例化")
    void apacheHttp5ClientOutboundTransformer_shouldInstantiate() {
        ApacheHttp5ClientOutboundTransformer transformer = new ApacheHttp5ClientOutboundTransformer();
        assertNotNull(transformer);
    }

    @Test
    @DisplayName("RocketMqProducerTransformer 应能正常实例化")
    void rocketMqProducerTransformer_shouldInstantiate() {
        RocketMqProducerTransformer transformer = new RocketMqProducerTransformer();
        assertNotNull(transformer);
    }
}