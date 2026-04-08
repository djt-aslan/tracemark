package io.tracemark.gray.autoconfigure.http;

import io.tracemark.gray.core.GrayConstants;
import io.tracemark.gray.core.GrayContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpRequest;
import org.springframework.http.client.ClientHttpRequestExecution;

import java.net.URI;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * GrayRestTemplateInterceptor 单元测试
 */
class GrayRestTemplateInterceptorTest {

    private GrayRestTemplateInterceptor interceptor;

    @BeforeEach
    void setUp() {
        interceptor = new GrayRestTemplateInterceptor();
        GrayContext.clear();
    }

    @AfterEach
    void tearDown() {
        GrayContext.clear();
    }

    @Nested
    @DisplayName("intercept 方法测试")
    class InterceptTest {

        @Test
        @DisplayName("灰度标存在时应注入 Header")
        void intercept_withGrayTag_shouldInjectHeader() throws Exception {
            GrayContext.set("gray-v1");

            HttpRequest request = mock(HttpRequest.class);
            HttpHeaders headers = new HttpHeaders();
            when(request.getHeaders()).thenReturn(headers);
            when(request.getURI()).thenReturn(URI.create("http://test"));

            ClientHttpRequestExecution execution = mock(ClientHttpRequestExecution.class);

            interceptor.intercept(request, new byte[0], execution);

            assertEquals("gray-v1", headers.getFirst(GrayConstants.HEADER_GRAY_TAG));
            verify(execution).execute(request, new byte[0]);
        }

        @Test
        @DisplayName("灰度标为 stable 时仍应注入 Header")
        void intercept_withStableTag_shouldInjectHeader() throws Exception {
            GrayContext.set(GrayConstants.TAG_STABLE);

            HttpRequest request = mock(HttpRequest.class);
            HttpHeaders headers = new HttpHeaders();
            when(request.getHeaders()).thenReturn(headers);
            when(request.getURI()).thenReturn(URI.create("http://test"));

            ClientHttpRequestExecution execution = mock(ClientHttpRequestExecution.class);

            interceptor.intercept(request, new byte[0], execution);

            assertEquals(GrayConstants.TAG_STABLE, headers.getFirst(GrayConstants.HEADER_GRAY_TAG));
        }

        @Test
        @DisplayName("无灰度标时应注入 stable Header")
        void intercept_withoutTag_shouldInjectStableHeader() throws Exception {
            GrayContext.clear();

            HttpRequest request = mock(HttpRequest.class);
            HttpHeaders headers = new HttpHeaders();
            when(request.getHeaders()).thenReturn(headers);
            when(request.getURI()).thenReturn(URI.create("http://test"));

            ClientHttpRequestExecution execution = mock(ClientHttpRequestExecution.class);

            interceptor.intercept(request, new byte[0], execution);

            // GrayContext.get() 默认返回 stable，所以会注入 stable
            assertEquals(GrayConstants.TAG_STABLE, headers.getFirst(GrayConstants.HEADER_GRAY_TAG));
            verify(execution).execute(request, new byte[0]);
        }
    }
}