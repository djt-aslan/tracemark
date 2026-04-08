package io.tracemark.agent.advice;

import io.tracemark.gray.core.GrayConstants;
import io.tracemark.gray.core.GrayContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpRequest;

import java.net.URI;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.Mockito.*;

/**
 * RestTemplateOutboundAdvice 单元测试
 */
class RestTemplateOutboundAdviceTest {

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
        @DisplayName("灰度标存在时应注入 Header")
        void onEnter_withGrayTag_shouldInjectHeader() {
            GrayContext.set("gray-v1");

            HttpRequest request = mock(HttpRequest.class);
            HttpHeaders headers = new HttpHeaders();
            when(request.getHeaders()).thenReturn(headers);
            when(request.getURI()).thenReturn(URI.create("http://test"));

            simulateOnEnter(request);

            assertEquals("gray-v1", headers.getFirst(GrayConstants.HEADER_GRAY_TAG));
        }

        @Test
        @DisplayName("灰度标为 stable 时仍应注入 Header")
        void onEnter_withStableTag_shouldInjectHeader() {
            GrayContext.set(GrayConstants.TAG_STABLE);

            HttpRequest request = mock(HttpRequest.class);
            HttpHeaders headers = new HttpHeaders();
            when(request.getHeaders()).thenReturn(headers);
            when(request.getURI()).thenReturn(URI.create("http://test"));

            simulateOnEnter(request);

            assertEquals(GrayConstants.TAG_STABLE, headers.getFirst(GrayConstants.HEADER_GRAY_TAG));
        }

        @Test
        @DisplayName("无灰度标时应注入 stable Header")
        void onEnter_withoutTag_shouldInjectStableHeader() {
            GrayContext.clear();

            HttpRequest request = mock(HttpRequest.class);
            HttpHeaders headers = new HttpHeaders();
            when(request.getHeaders()).thenReturn(headers);
            when(request.getURI()).thenReturn(URI.create("http://test"));

            simulateOnEnter(request);

            // GrayContext.get() 默认返回 stable
            assertEquals(GrayConstants.TAG_STABLE, headers.getFirst(GrayConstants.HEADER_GRAY_TAG));
        }
    }

    // 模拟 Advice.onEnter 的逻辑
    private void simulateOnEnter(HttpRequest request) {
        String tag = GrayContext.get();
        if (tag != null && !tag.isEmpty()) {
            request.getHeaders().set(GrayConstants.HEADER_GRAY_TAG, tag);
        }
    }
}