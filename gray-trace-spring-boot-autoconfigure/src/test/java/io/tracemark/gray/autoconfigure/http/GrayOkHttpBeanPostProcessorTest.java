package io.tracemark.gray.autoconfigure.http;

import io.tracemark.gray.core.GrayProperties;
import okhttp3.OkHttpClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * GrayOkHttpBeanPostProcessor 单元测试
 */
class GrayOkHttpBeanPostProcessorTest {

    private GrayOkHttpBeanPostProcessor processor;
    private GrayProperties properties;

    @BeforeEach
    void setUp() {
        properties = new GrayProperties();
        processor = new GrayOkHttpBeanPostProcessor(properties);
    }

    @Nested
    @DisplayName("postProcessAfterInitialization 方法测试")
    class PostProcessTest {

        @Test
        @DisplayName("非 OkHttpClient Bean 应直接返回")
        void postProcess_nonOkHttpClient_shouldReturnOriginal() {
            Object original = new Object();
            Object result = processor.postProcessAfterInitialization(original, "testBean");
            assertSame(original, result);
        }

        @Test
        @DisplayName("OkHttpClient Bean 应注入拦截器")
        void postProcess_okHttpClient_shouldAddInterceptor() {
            OkHttpClient client = new OkHttpClient.Builder().build();
            int originalCount = client.interceptors().size();

            Object result = processor.postProcessAfterInitialization(client, "okHttpClient");

            assertNotSame(client, result);
            assertTrue(result instanceof OkHttpClient);
            OkHttpClient processed = (OkHttpClient) result;
            assertEquals(originalCount + 1, processed.interceptors().size());
            assertTrue(processed.interceptors().stream()
                    .anyMatch(i -> i instanceof GrayOkHttpInterceptor));
        }

        @Test
        @DisplayName("已注入拦截器的 OkHttpClient 不应重复注入")
        void postProcess_alreadyInjected_shouldNotDuplicate() {
            OkHttpClient client = new OkHttpClient.Builder().build();
            OkHttpClient firstProcessed = (OkHttpClient) processor.postProcessAfterInitialization(client, "okHttpClient");
            int countAfterFirst = firstProcessed.interceptors().size();

            OkHttpClient secondProcessed = (OkHttpClient) processor.postProcessAfterInitialization(firstProcessed, "okHttpClient");

            assertEquals(countAfterFirst, secondProcessed.interceptors().size());
        }

        @Test
        @DisplayName("全局禁用时不注入拦截器")
        void postProcess_globalDisabled_shouldNotAddInterceptor() {
            properties.setEnabled(false);
            OkHttpClient client = new OkHttpClient.Builder().build();
            int originalCount = client.interceptors().size();

            Object result = processor.postProcessAfterInitialization(client, "okHttpClient");

            OkHttpClient processed = (OkHttpClient) result;
            assertEquals(originalCount, processed.interceptors().size());
        }

        @Test
        @DisplayName("OkHttp 配置禁用时不注入拦截器")
        void postProcess_okHttpDisabled_shouldNotAddInterceptor() {
            properties.getOkHttp().setEnabled(false);
            OkHttpClient client = new OkHttpClient.Builder().build();
            int originalCount = client.interceptors().size();

            Object result = processor.postProcessAfterInitialization(client, "okHttpClient");

            OkHttpClient processed = (OkHttpClient) result;
            assertEquals(originalCount, processed.interceptors().size());
        }
    }
}
