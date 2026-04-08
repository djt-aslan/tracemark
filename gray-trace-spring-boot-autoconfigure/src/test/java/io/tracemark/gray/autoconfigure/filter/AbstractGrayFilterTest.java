package io.tracemark.gray.autoconfigure.filter;

import io.tracemark.gray.core.GrayConstants;
import io.tracemark.gray.core.GrayContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * AbstractGrayFilter 单元测试
 */
class AbstractGrayFilterTest {

    private AbstractGrayFilter filter;

    @BeforeEach
    void setUp() {
        filter = new TestGrayFilter();
        GrayContext.clear();
    }

    @AfterEach
    void tearDown() {
        GrayContext.clear();
    }

    @Nested
    @DisplayName("resolveTag 方法测试")
    class ResolveTagTest {

        @Test
        @DisplayName("null 输入返回 stable")
        void resolveTag_null_shouldReturnStable() {
            assertEquals(GrayConstants.TAG_STABLE, filter.resolveTag(null));
        }

        @Test
        @DisplayName("空字符串返回 stable")
        void resolveTag_empty_shouldReturnStable() {
            assertEquals(GrayConstants.TAG_STABLE, filter.resolveTag(""));
        }

        @Test
        @DisplayName("有效灰度标返回原值")
        void resolveTag_validTag_shouldReturnOriginal() {
            assertEquals("gray-v1", filter.resolveTag("gray-v1"));
        }
    }

    @Nested
    @DisplayName("doGrayFilter 方法测试")
    class DoGrayFilterTest {

        @Test
        @DisplayName("应正确设置灰度上下文")
        void doGrayFilter_shouldSetContext() throws Exception {
            StringBuilder capturedTag = new StringBuilder();
            filter.doGrayFilter("gray-v1", () -> {
                capturedTag.append(GrayContext.get());
            });
            assertEquals("gray-v1", capturedTag.toString());
        }

        @Test
        @DisplayName("执行完毕应清除上下文")
        void doGrayFilter_shouldClearContext() throws Exception {
            filter.doGrayFilter("gray-v1", () -> {});
            assertEquals(GrayConstants.TAG_STABLE, GrayContext.get());
        }

        @Test
        @DisplayName("异常时应清除上下文")
        void doGrayFilter_shouldClearContextOnException() throws Exception {
            try {
                filter.doGrayFilter("gray-v1", () -> {
                    throw new RuntimeException("test exception");
                });
            } catch (RuntimeException e) {
                // expected
            }
            assertEquals(GrayConstants.TAG_STABLE, GrayContext.get());
        }
    }

    // 测试用的具体实现
    private static class TestGrayFilter extends AbstractGrayFilter {}
}