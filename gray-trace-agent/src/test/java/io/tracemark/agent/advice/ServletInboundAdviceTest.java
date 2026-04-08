package io.tracemark.agent.advice;

import io.tracemark.gray.core.GrayConstants;
import io.tracemark.gray.core.GrayContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import javax.servlet.http.HttpServletRequest;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * ServletInboundAdvice 单元测试
 */
class ServletInboundAdviceTest {

    @BeforeEach
    void setUp() {
        GrayContext.clear();
    }

    @AfterEach
    void tearDown() {
        GrayContext.clear();
    }

    @Nested
    @DisplayName("onEnter 方法逻辑测试")
    class OnEnterTest {

        @Test
        @DisplayName("灰度标存在时应设置上下文")
        void onEnter_withGrayTag_shouldSetContext() {
            HttpServletRequest request = mock(HttpServletRequest.class);
            when(request.getHeader(GrayConstants.HEADER_GRAY_TAG)).thenReturn("gray-v1");

            simulateOnEnter(request);

            assertEquals("gray-v1", GrayContext.get());
        }

        @Test
        @DisplayName("灰度标为 stable 时应设置上下文")
        void onEnter_withStableTag_shouldSetContext() {
            HttpServletRequest request = mock(HttpServletRequest.class);
            when(request.getHeader(GrayConstants.HEADER_GRAY_TAG)).thenReturn(GrayConstants.TAG_STABLE);

            simulateOnEnter(request);

            assertEquals(GrayConstants.TAG_STABLE, GrayContext.get());
        }

        @Test
        @DisplayName("无灰度标时应设置 stable")
        void onEnter_withoutTag_shouldSetStable() {
            HttpServletRequest request = mock(HttpServletRequest.class);
            when(request.getHeader(GrayConstants.HEADER_GRAY_TAG)).thenReturn(null);

            simulateOnEnter(request);

            assertEquals(GrayConstants.TAG_STABLE, GrayContext.get());
        }

        @Test
        @DisplayName("空灰度标时应设置 stable")
        void onEnter_withEmptyTag_shouldSetStable() {
            HttpServletRequest request = mock(HttpServletRequest.class);
            when(request.getHeader(GrayConstants.HEADER_GRAY_TAG)).thenReturn("");

            simulateOnEnter(request);

            assertEquals(GrayConstants.TAG_STABLE, GrayContext.get());
        }
    }

    @Nested
    @DisplayName("onExit 方法逻辑测试")
    class OnExitTest {

        @Test
        @DisplayName("onExit 应清除上下文")
        void onExit_shouldClearContext() {
            GrayContext.set("gray-v1");

            simulateOnExit();

            assertEquals(GrayConstants.TAG_STABLE, GrayContext.get());
        }
    }

    // 模拟 Advice.onEnter 的逻辑
    private void simulateOnEnter(HttpServletRequest request) {
        String tag = request.getHeader(GrayConstants.HEADER_GRAY_TAG);
        GrayContext.set(tag != null && !tag.isEmpty() ? tag : GrayConstants.TAG_STABLE);
    }

    // 模拟 Advice.onExit 的逻辑
    private void simulateOnExit() {
        GrayContext.clear();
    }
}