package io.tracemark.gray.core;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * GrayProperties 配置属性测试
 */
class GrayPropertiesTest {

    @Test
    @DisplayName("默认 enabled 应为 true")
    void defaultEnabled_shouldBeTrue() {
        GrayProperties props = new GrayProperties();
        assertTrue(props.isEnabled());
    }

    @Nested
    @DisplayName("主配置 getters/setters 测试")
    class MainConfigTest {

        @Test
        @DisplayName("enabled 属性可设置")
        void enabled_canSet() {
            GrayProperties props = new GrayProperties();
            props.setEnabled(false);
            assertFalse(props.isEnabled());
        }

        @Test
        @DisplayName("所有嵌套配置应有默认实例")
        void nestedConfigs_shouldHaveDefaults() {
            GrayProperties props = new GrayProperties();
            assertNotNull(props.getServlet());
            assertNotNull(props.getRestTemplate());
            assertNotNull(props.getOkHttp());
            assertNotNull(props.getHttpClient());
            assertNotNull(props.getFeign());
            assertNotNull(props.getThreadPool());
            assertNotNull(props.getMq());
            assertNotNull(props.getWebFlux());
            assertNotNull(props.getApacheHttpClient());
            assertNotNull(props.getCompletableFuture());
        }
    }

    @Nested
    @DisplayName("Servlet 配置测试")
    class ServletConfigTest {

        @Test
        @DisplayName("默认 enabled 为 true")
        void defaultEnabled_shouldBeTrue() {
            assertTrue(new GrayProperties.Servlet().isEnabled());
        }

        @Test
        @DisplayName("可设置为 false")
        void canSetDisabled() {
            GrayProperties.Servlet servlet = new GrayProperties.Servlet();
            servlet.setEnabled(false);
            assertFalse(servlet.isEnabled());
        }
    }

    @Nested
    @DisplayName("RestTemplate 配置测试")
    class RestTemplateConfigTest {

        @Test
        @DisplayName("默认 enabled 为 true")
        void defaultEnabled_shouldBeTrue() {
            assertTrue(new GrayProperties.RestTemplate().isEnabled());
        }

        @Test
        @DisplayName("可设置为 false")
        void canSetDisabled() {
            GrayProperties.RestTemplate restTemplate = new GrayProperties.RestTemplate();
            restTemplate.setEnabled(false);
            assertFalse(restTemplate.isEnabled());
        }
    }

    @Nested
    @DisplayName("OkHttp 配置测试")
    class OkHttpConfigTest {

        @Test
        @DisplayName("默认 enabled 为 true")
        void defaultEnabled_shouldBeTrue() {
            assertTrue(new GrayProperties.OkHttp().isEnabled());
        }

        @Test
        @DisplayName("可设置为 false")
        void canSetDisabled() {
            GrayProperties.OkHttp okHttp = new GrayProperties.OkHttp();
            okHttp.setEnabled(false);
            assertFalse(okHttp.isEnabled());
        }
    }

    @Nested
    @DisplayName("HttpClient 配置测试")
    class HttpClientConfigTest {

        @Test
        @DisplayName("默认 enabled 为 true")
        void defaultEnabled_shouldBeTrue() {
            assertTrue(new GrayProperties.HttpClient().isEnabled());
        }

        @Test
        @DisplayName("可设置为 false")
        void canSetDisabled() {
            GrayProperties.HttpClient httpClient = new GrayProperties.HttpClient();
            httpClient.setEnabled(false);
            assertFalse(httpClient.isEnabled());
        }
    }

    @Nested
    @DisplayName("Feign 配置测试")
    class FeignConfigTest {

        @Test
        @DisplayName("默认 enabled 为 true")
        void defaultEnabled_shouldBeTrue() {
            assertTrue(new GrayProperties.Feign().isEnabled());
        }

        @Test
        @DisplayName("可设置为 false")
        void canSetDisabled() {
            GrayProperties.Feign feign = new GrayProperties.Feign();
            feign.setEnabled(false);
            assertFalse(feign.isEnabled());
        }
    }

    @Nested
    @DisplayName("ThreadPool 配置测试")
    class ThreadPoolConfigTest {

        @Test
        @DisplayName("默认 enabled 为 true")
        void defaultEnabled_shouldBeTrue() {
            assertTrue(new GrayProperties.ThreadPool().isEnabled());
        }

        @Test
        @DisplayName("默认 asyncDecorator 为 true")
        void defaultAsyncDecorator_shouldBeTrue() {
            assertTrue(new GrayProperties.ThreadPool().isAsyncDecorator());
        }

        @Test
        @DisplayName("可设置 enabled 为 false")
        void canSetDisabled() {
            GrayProperties.ThreadPool threadPool = new GrayProperties.ThreadPool();
            threadPool.setEnabled(false);
            assertFalse(threadPool.isEnabled());
        }

        @Test
        @DisplayName("可设置 asyncDecorator 为 false")
        void canSetAsyncDecoratorDisabled() {
            GrayProperties.ThreadPool threadPool = new GrayProperties.ThreadPool();
            threadPool.setAsyncDecorator(false);
            assertFalse(threadPool.isAsyncDecorator());
        }
    }

    @Nested
    @DisplayName("MQ 配置测试")
    class MqConfigTest {

        @Test
        @DisplayName("默认 enabled 为 false")
        void defaultEnabled_shouldBeFalse() {
            assertFalse(new GrayProperties.Mq().isEnabled());
        }

        @Test
        @DisplayName("默认 producer 为 true")
        void defaultProducer_shouldBeTrue() {
            assertTrue(new GrayProperties.Mq().isProducer());
        }

        @Test
        @DisplayName("默认 consumer 为 true")
        void defaultConsumer_shouldBeTrue() {
            assertTrue(new GrayProperties.Mq().isConsumer());
        }

        @Test
        @DisplayName("可设置 enabled 为 true")
        void canSetEnabled() {
            GrayProperties.Mq mq = new GrayProperties.Mq();
            mq.setEnabled(true);
            assertTrue(mq.isEnabled());
        }

        @Test
        @DisplayName("可设置 producer 为 false")
        void canSetProducerDisabled() {
            GrayProperties.Mq mq = new GrayProperties.Mq();
            mq.setProducer(false);
            assertFalse(mq.isProducer());
        }

        @Test
        @DisplayName("可设置 consumer 为 false")
        void canSetConsumerDisabled() {
            GrayProperties.Mq mq = new GrayProperties.Mq();
            mq.setConsumer(false);
            assertFalse(mq.isConsumer());
        }
    }

    @Nested
    @DisplayName("WebFlux 配置测试")
    class WebFluxConfigTest {

        @Test
        @DisplayName("默认 enabled 为 true")
        void defaultEnabled_shouldBeTrue() {
            assertTrue(new GrayProperties.WebFlux().isEnabled());
        }

        @Test
        @DisplayName("可设置为 false")
        void canSetDisabled() {
            GrayProperties.WebFlux webFlux = new GrayProperties.WebFlux();
            webFlux.setEnabled(false);
            assertFalse(webFlux.isEnabled());
        }
    }

    @Nested
    @DisplayName("ApacheHttpClient 配置测试")
    class ApacheHttpClientConfigTest {

        @Test
        @DisplayName("默认 enabled 为 true")
        void defaultEnabled_shouldBeTrue() {
            assertTrue(new GrayProperties.ApacheHttpClient().isEnabled());
        }

        @Test
        @DisplayName("可设置为 false")
        void canSetDisabled() {
            GrayProperties.ApacheHttpClient apacheHttpClient = new GrayProperties.ApacheHttpClient();
            apacheHttpClient.setEnabled(false);
            assertFalse(apacheHttpClient.isEnabled());
        }
    }

    @Nested
    @DisplayName("CompletableFuture 配置测试")
    class CompletableFutureConfigTest {

        @Test
        @DisplayName("默认 enabled 为 true")
        void defaultEnabled_shouldBeTrue() {
            assertTrue(new GrayProperties.CompletableFuture().isEnabled());
        }

        @Test
        @DisplayName("可设置为 false")
        void canSetDisabled() {
            GrayProperties.CompletableFuture completableFuture = new GrayProperties.CompletableFuture();
            completableFuture.setEnabled(false);
            assertFalse(completableFuture.isEnabled());
        }
    }

    @Nested
    @DisplayName("嵌套配置 setters 测试")
    class NestedSettersTest {

        @Test
        @DisplayName("可设置 Servlet 配置")
        void canSetServlet() {
            GrayProperties props = new GrayProperties();
            GrayProperties.Servlet servlet = new GrayProperties.Servlet();
            servlet.setEnabled(false);
            props.setServlet(servlet);
            assertFalse(props.getServlet().isEnabled());
        }

        @Test
        @DisplayName("可设置 RestTemplate 配置")
        void canSetRestTemplate() {
            GrayProperties props = new GrayProperties();
            GrayProperties.RestTemplate restTemplate = new GrayProperties.RestTemplate();
            restTemplate.setEnabled(false);
            props.setRestTemplate(restTemplate);
            assertFalse(props.getRestTemplate().isEnabled());
        }

        @Test
        @DisplayName("可设置 OkHttp 配置")
        void canSetOkHttp() {
            GrayProperties props = new GrayProperties();
            GrayProperties.OkHttp okHttp = new GrayProperties.OkHttp();
            okHttp.setEnabled(false);
            props.setOkHttp(okHttp);
            assertFalse(props.getOkHttp().isEnabled());
        }

        @Test
        @DisplayName("可设置 HttpClient 配置")
        void canSetHttpClient() {
            GrayProperties props = new GrayProperties();
            GrayProperties.HttpClient httpClient = new GrayProperties.HttpClient();
            httpClient.setEnabled(false);
            props.setHttpClient(httpClient);
            assertFalse(props.getHttpClient().isEnabled());
        }

        @Test
        @DisplayName("可设置 Feign 配置")
        void canSetFeign() {
            GrayProperties props = new GrayProperties();
            GrayProperties.Feign feign = new GrayProperties.Feign();
            feign.setEnabled(false);
            props.setFeign(feign);
            assertFalse(props.getFeign().isEnabled());
        }

        @Test
        @DisplayName("可设置 ThreadPool 配置")
        void canSetThreadPool() {
            GrayProperties props = new GrayProperties();
            GrayProperties.ThreadPool threadPool = new GrayProperties.ThreadPool();
            threadPool.setEnabled(false);
            props.setThreadPool(threadPool);
            assertFalse(props.getThreadPool().isEnabled());
        }

        @Test
        @DisplayName("可设置 Mq 配置")
        void canSetMq() {
            GrayProperties props = new GrayProperties();
            GrayProperties.Mq mq = new GrayProperties.Mq();
            mq.setEnabled(true);
            props.setMq(mq);
            assertTrue(props.getMq().isEnabled());
        }

        @Test
        @DisplayName("可设置 WebFlux 配置")
        void canSetWebFlux() {
            GrayProperties props = new GrayProperties();
            GrayProperties.WebFlux webFlux = new GrayProperties.WebFlux();
            webFlux.setEnabled(false);
            props.setWebFlux(webFlux);
            assertFalse(props.getWebFlux().isEnabled());
        }

        @Test
        @DisplayName("可设置 ApacheHttpClient 配置")
        void canSetApacheHttpClient() {
            GrayProperties props = new GrayProperties();
            GrayProperties.ApacheHttpClient apacheHttpClient = new GrayProperties.ApacheHttpClient();
            apacheHttpClient.setEnabled(false);
            props.setApacheHttpClient(apacheHttpClient);
            assertFalse(props.getApacheHttpClient().isEnabled());
        }

        @Test
        @DisplayName("可设置 CompletableFuture 配置")
        void canSetCompletableFuture() {
            GrayProperties props = new GrayProperties();
            GrayProperties.CompletableFuture completableFuture = new GrayProperties.CompletableFuture();
            completableFuture.setEnabled(false);
            props.setCompletableFuture(completableFuture);
            assertFalse(props.getCompletableFuture().isEnabled());
        }
    }
}