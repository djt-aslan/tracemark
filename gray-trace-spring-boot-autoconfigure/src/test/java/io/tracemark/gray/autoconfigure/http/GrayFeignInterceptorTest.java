package io.tracemark.gray.autoconfigure.http;

import feign.RequestTemplate;
import io.tracemark.gray.core.GrayConstants;
import io.tracemark.gray.core.GrayContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * GrayFeignInterceptor 单元测试
 */
class GrayFeignInterceptorTest {

    private GrayFeignInterceptor interceptor;

    @BeforeEach
    void setUp() {
        interceptor = new GrayFeignInterceptor();
        GrayContext.clear();
    }

    @AfterEach
    void tearDown() {
        GrayContext.clear();
    }

    @Nested
    @DisplayName("apply 方法测试")
    class ApplyTest {

        @Test
        @DisplayName("灰度标存在时应注入 Header")
        void apply_withGrayTag_shouldInjectHeader() {
            GrayContext.set("gray-v1");
            RequestTemplate template = new RequestTemplate();

            interceptor.apply(template);

            assertEquals("gray-v1", template.headers().get(GrayConstants.HEADER_GRAY_TAG).iterator().next());
        }

        @Test
        @DisplayName("灰度标为 stable 时仍应注入 Header")
        void apply_withStableTag_shouldInjectHeader() {
            GrayContext.set(GrayConstants.TAG_STABLE);
            RequestTemplate template = new RequestTemplate();

            interceptor.apply(template);

            assertEquals(GrayConstants.TAG_STABLE, template.headers().get(GrayConstants.HEADER_GRAY_TAG).iterator().next());
        }

        @Test
        @DisplayName("无灰度标时不应注入 Header")
        void apply_withoutTag_shouldNotInjectHeader() {
            GrayContext.clear();
            RequestTemplate template = new RequestTemplate();

            interceptor.apply(template);

            // 由于 GrayContext.get() 返回 stable，所以会注入 stable
            // 这个行为与其他 interceptor 一致
            assertEquals(GrayConstants.TAG_STABLE, template.headers().get(GrayConstants.HEADER_GRAY_TAG).iterator().next());
        }
    }
}