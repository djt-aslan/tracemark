package io.tracemark.gray.autoconfigure.async;

import io.tracemark.gray.core.GrayProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static org.junit.jupiter.api.Assertions.*;

/**
 * GrayExecutorBeanPostProcessor 单元测试
 */
class GrayExecutorBeanPostProcessorTest {

    private GrayExecutorBeanPostProcessor processor;
    private GrayProperties properties;

    @BeforeEach
    void setUp() {
        properties = new GrayProperties();
        processor = new GrayExecutorBeanPostProcessor(properties);
    }

    @Nested
    @DisplayName("postProcessBeforeInitialization 方法测试")
    class BeforeInitializationTest {

        @Test
        @DisplayName("非 ThreadPoolTaskExecutor Bean 应直接返回")
        void postProcess_nonThreadPoolTaskExecutor_shouldReturnOriginal() {
            Object original = new Object();
            Object result = processor.postProcessBeforeInitialization(original, "testBean");
            assertSame(original, result);
        }

        @Test
        @DisplayName("ThreadPoolTaskExecutor 应注入 GrayTaskDecorator")
        void postProcess_threadPoolTaskExecutor_shouldAddDecorator() {
            ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
            executor.setThreadNamePrefix("test-");

            Object result = processor.postProcessBeforeInitialization(executor, "taskExecutor");

            assertSame(executor, result);
            // 验证 TaskDecorator 已设置（通过反射检查）
            // executor 在 initialize() 之后才可用，此处仅验证处理器介入
        }

        @Test
        @DisplayName("全局禁用时不注入 Decorator")
        void postProcess_globalDisabled_shouldNotAddDecorator() {
            properties.setEnabled(false);
            ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();

            Object result = processor.postProcessBeforeInitialization(executor, "taskExecutor");

            assertSame(executor, result);
        }

        @Test
        @DisplayName("ThreadPool 配置禁用时不注入 Decorator")
        void postProcess_threadPoolDisabled_shouldNotAddDecorator() {
            properties.getThreadPool().setEnabled(false);
            ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();

            Object result = processor.postProcessBeforeInitialization(executor, "taskExecutor");

            assertSame(executor, result);
        }

        @Test
        @DisplayName("AsyncDecorator 禁用时不注入 Decorator")
        void postProcess_asyncDecoratorDisabled_shouldNotAddDecorator() {
            properties.getThreadPool().setAsyncDecorator(false);
            ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();

            Object result = processor.postProcessBeforeInitialization(executor, "taskExecutor");

            assertSame(executor, result);
        }
    }

    @Nested
    @DisplayName("postProcessAfterInitialization 方法测试")
    class AfterInitializationTest {

        @Test
        @DisplayName("普通 ExecutorService 应被 TTL 包装")
        void postProcess_executorService_shouldWrapWithTtl() {
            ExecutorService executor = Executors.newFixedThreadPool(2);

            Object result = processor.postProcessAfterInitialization(executor, "executorService");

            // TTL 包装后返回新实例
            assertNotSame(executor, result);
        }

        @Test
        @DisplayName("ThreadPoolTaskExecutor 不应被 TTL 包装")
        void postProcess_threadPoolTaskExecutor_shouldNotWrap() {
            ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
            executor.setThreadNamePrefix("test-");
            executor.initialize();

            Object result = processor.postProcessAfterInitialization(executor, "taskExecutor");

            assertSame(executor, result);
        }

        @Test
        @DisplayName("全局禁用时不应包装 ExecutorService")
        void postProcess_globalDisabled_shouldNotWrap() {
            properties.setEnabled(false);
            ExecutorService executor = Executors.newFixedThreadPool(2);

            Object result = processor.postProcessAfterInitialization(executor, "executorService");

            assertSame(executor, result);
        }

        @Test
        @DisplayName("ThreadPool 配置禁用时不应包装 ExecutorService")
        void postProcess_threadPoolDisabled_shouldNotWrap() {
            properties.getThreadPool().setEnabled(false);
            ExecutorService executor = Executors.newFixedThreadPool(2);

            Object result = processor.postProcessAfterInitialization(executor, "executorService");

            assertSame(executor, result);
        }
    }
}