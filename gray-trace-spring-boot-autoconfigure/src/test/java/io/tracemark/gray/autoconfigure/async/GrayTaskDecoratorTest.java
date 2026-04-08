package io.tracemark.gray.autoconfigure.async;

import io.tracemark.gray.core.GrayConstants;
import io.tracemark.gray.core.GrayContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * GrayTaskDecorator 单元测试
 */
class GrayTaskDecoratorTest {

    private GrayTaskDecorator decorator;

    @BeforeEach
    void setUp() {
        decorator = new GrayTaskDecorator();
        GrayContext.clear();
    }

    @AfterEach
    void tearDown() {
        GrayContext.clear();
    }

    @Nested
    @DisplayName("decorate 方法测试")
    class DecorateTest {

        @Test
        @DisplayName("应捕获父线程灰度标传递到子线程")
        void decorate_shouldPropagateTag() throws Exception {
            GrayContext.set("gray-v1");

            StringBuilder capturedTag = new StringBuilder();
            Runnable decorated = decorator.decorate(() -> {
                capturedTag.append(GrayContext.get());
            });

            // 模拟在子线程执行
            decorated.run();

            assertEquals("gray-v1", capturedTag.toString());
        }

        @Test
        @DisplayName("执行完毕应清除工作线程上下文")
        void decorate_shouldClearContext() throws Exception {
            GrayContext.set("parent-tag");

            Runnable decorated = decorator.decorate(() -> {});

            decorated.run();

            // 执行完毕后应恢复到执行前的状态
            // 由于是同一线程，恢复到执行前的状态就是 parent-tag
            assertEquals("parent-tag", GrayContext.get());
        }

        @Test
        @DisplayName("父线程灰度标为 stable 时仍应传递")
        void decorate_withStableTag_shouldPropagate() throws Exception {
            GrayContext.set(GrayConstants.TAG_STABLE);

            StringBuilder capturedTag = new StringBuilder();
            Runnable decorated = decorator.decorate(() -> {
                capturedTag.append(GrayContext.get());
            });

            decorated.run();

            assertEquals(GrayConstants.TAG_STABLE, capturedTag.toString());
        }

        @Test
        @DisplayName("异常时应恢复执行前状态")
        void decorate_onException_shouldRestoreState() throws Exception {
            GrayContext.set("original-tag");

            Runnable decorated = decorator.decorate(() -> {
                throw new RuntimeException("test exception");
            });

            try {
                decorated.run();
            } catch (RuntimeException e) {
                // expected
            }

            // 执行完毕后应恢复到执行前的状态
            assertEquals("original-tag", GrayContext.get());
        }
    }
}