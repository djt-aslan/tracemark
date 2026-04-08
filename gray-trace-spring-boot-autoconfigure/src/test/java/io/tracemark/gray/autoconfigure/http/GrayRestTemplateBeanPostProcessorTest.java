package io.tracemark.gray.autoconfigure.http;

import io.tracemark.gray.core.GrayProperties;
import io.tracemark.gray.core.GrayContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.web.client.RestTemplate;

import static org.junit.jupiter.api.Assertions.*;

/**
 * GrayRestTemplateBeanPostProcessor 单元测试
 */
class GrayRestTemplateBeanPostProcessorTest {

    private GrayRestTemplateBeanPostProcessor processor;
    private GrayProperties properties;

    @BeforeEach
    void setUp() {
        properties = new GrayProperties();
        processor = new GrayRestTemplateBeanPostProcessor(properties);
        GrayContext.clear();
    }

    @AfterEach
    void tearDown() {
        GrayContext.clear();
    }

    @Nested
    @DisplayName("postProcessAfterInitialization 方法测试")
    class PostProcessTest {

        @Test
        @DisplayName("非 RestTemplate Bean 应直接返回")
        void postProcess_nonRestTemplate_shouldReturnOriginal() {
            Object original = new Object();
            Object result = processor.postProcessAfterInitialization(original, "testBean");
            assertSame(original, result);
        }

        @Test
        @DisplayName("RestTemplate Bean 应注入拦截器")
        void postProcess_restTemplate_shouldAddInterceptor() {
            RestTemplate restTemplate = new RestTemplate();
            int originalCount = restTemplate.getInterceptors().size();

            Object result = processor.postProcessAfterInitialization(restTemplate, "restTemplate");

            assertSame(restTemplate, result);
            assertEquals(originalCount + 1, restTemplate.getInterceptors().size());
            assertTrue(restTemplate.getInterceptors().stream()
                    .anyMatch(i -> i instanceof GrayRestTemplateInterceptor));
        }

        @Test
        @DisplayName("已注入拦截器的 RestTemplate 不应重复注入")
        void postProcess_alreadyInjected_shouldNotDuplicate() {
            RestTemplate restTemplate = new RestTemplate();
            processor.postProcessAfterInitialization(restTemplate, "restTemplate");
            int countAfterFirst = restTemplate.getInterceptors().size();

            processor.postProcessAfterInitialization(restTemplate, "restTemplate");

            assertEquals(countAfterFirst, restTemplate.getInterceptors().size());
        }

        @Test
        @DisplayName("全局禁用时不注入拦截器")
        void postProcess_globalDisabled_shouldNotAddInterceptor() {
            properties.setEnabled(false);
            RestTemplate restTemplate = new RestTemplate();
            int originalCount = restTemplate.getInterceptors().size();

            processor.postProcessAfterInitialization(restTemplate, "restTemplate");

            assertEquals(originalCount, restTemplate.getInterceptors().size());
        }

        @Test
        @DisplayName("RestTemplate 配置禁用时不注入拦截器")
        void postProcess_restTemplateDisabled_shouldNotAddInterceptor() {
            properties.getRestTemplate().setEnabled(false);
            RestTemplate restTemplate = new RestTemplate();
            int originalCount = restTemplate.getInterceptors().size();

            processor.postProcessAfterInitialization(restTemplate, "restTemplate");

            assertEquals(originalCount, restTemplate.getInterceptors().size());
        }
    }
}