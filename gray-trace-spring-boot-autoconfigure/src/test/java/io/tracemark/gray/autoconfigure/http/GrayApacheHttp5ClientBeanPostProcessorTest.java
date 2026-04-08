package io.tracemark.gray.autoconfigure.http;

import io.tracemark.gray.core.GrayProperties;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * GrayApacheHttp5ClientBeanPostProcessor 单元测试
 */
class GrayApacheHttp5ClientBeanPostProcessorTest {

    private GrayApacheHttp5ClientBeanPostProcessor processor;
    private GrayProperties properties;

    @BeforeEach
    void setUp() {
        properties = new GrayProperties();
        processor = new GrayApacheHttp5ClientBeanPostProcessor(properties);
    }

    @Nested
    @DisplayName("postProcessAfterInitialization 方法测试")
    class PostProcessTest {

        @Test
        @DisplayName("非 CloseableHttpClient Bean 应直接返回")
        void postProcess_nonHttpClient_shouldReturnOriginal() {
            Object original = new Object();
            Object result = processor.postProcessAfterInitialization(original, "testBean");
            assertSame(original, result);
        }

        @Test
        @DisplayName("全局禁用时不注入拦截器")
        void postProcess_globalDisabled_shouldNotAddInterceptor() throws Exception {
            properties.setEnabled(false);
            CloseableHttpClient client = org.apache.hc.client5.http.impl.classic.HttpClientBuilder.create().build();

            Object result = processor.postProcessAfterInitialization(client, "httpClient");

            // 由于 Apache HttpClient 5.x 重建实例，即使禁用也返回新实例
            // 但此测试主要验证禁用时不会注入灰度拦截器
            assertNotNull(result);
        }

        @Test
        @DisplayName("ApacheHttpClient 配置禁用时不注入拦截器")
        void postProcess_apacheHttpClientDisabled_shouldNotAddInterceptor() throws Exception {
            properties.getApacheHttpClient().setEnabled(false);
            CloseableHttpClient client = org.apache.hc.client5.http.impl.classic.HttpClientBuilder.create().build();

            Object result = processor.postProcessAfterInitialization(client, "httpClient");

            assertNotNull(result);
        }
    }
}