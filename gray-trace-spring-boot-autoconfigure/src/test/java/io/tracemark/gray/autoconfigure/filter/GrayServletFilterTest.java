package io.tracemark.gray.autoconfigure.filter;

import io.tracemark.gray.core.GrayConstants;
import io.tracemark.gray.core.GrayContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * GrayServletFilter 单元测试
 */
class GrayServletFilterTest {

    private GrayServletFilter filter;

    @BeforeEach
    void setUp() {
        filter = new GrayServletFilter();
        GrayContext.clear();
    }

    @AfterEach
    void tearDown() {
        GrayContext.clear();
    }

    @Nested
    @DisplayName("doFilter 方法测试")
    class DoFilterTest {

        @Test
        @DisplayName("灰度标存在时应设置上下文")
        void doFilter_withGrayTag_shouldSetContext() throws Exception {
            HttpServletRequest request = mock(HttpServletRequest.class);
            when(request.getHeader(GrayConstants.HEADER_GRAY_TAG)).thenReturn("gray-v1");
            HttpServletResponse response = mock(HttpServletResponse.class);
            FilterChain chain = mock(FilterChain.class);

            filter.doFilter(request, response, chain);

            verify(chain).doFilter(request, response);
        }

        @Test
        @DisplayName("灰度标为空时应设置 stable")
        void doFilter_withEmptyTag_shouldSetStable() throws Exception {
            HttpServletRequest request = mock(HttpServletRequest.class);
            when(request.getHeader(GrayConstants.HEADER_GRAY_TAG)).thenReturn(null);
            HttpServletResponse response = mock(HttpServletResponse.class);
            FilterChain chain = mock(FilterChain.class);

            filter.doFilter(request, response, chain);

            verify(chain).doFilter(request, response);
        }

        @Test
        @DisplayName("非 HttpServletRequest 应设置 stable")
        void doFilter_nonHttpServletRequest_shouldSetStable() throws Exception {
            javax.servlet.ServletRequest request = mock(javax.servlet.ServletRequest.class);
            javax.servlet.ServletResponse response = mock(javax.servlet.ServletResponse.class);
            FilterChain chain = mock(FilterChain.class);

            filter.doFilter(request, response, chain);

            verify(chain).doFilter(request, response);
        }

        @Test
        @DisplayName("请求结束后应清除上下文")
        void doFilter_shouldClearContext() throws Exception {
            HttpServletRequest request = mock(HttpServletRequest.class);
            when(request.getHeader(GrayConstants.HEADER_GRAY_TAG)).thenReturn("gray-v1");
            HttpServletResponse response = mock(HttpServletResponse.class);
            FilterChain chain = mock(FilterChain.class);

            filter.doFilter(request, response, chain);

            // 清除后默认返回 stable
            assertEquals(GrayConstants.TAG_STABLE, GrayContext.get());
        }

        @Test
        @DisplayName("异常时应清除上下文")
        void doFilter_onException_shouldClearContext() throws Exception {
            HttpServletRequest request = mock(HttpServletRequest.class);
            when(request.getHeader(GrayConstants.HEADER_GRAY_TAG)).thenReturn("gray-v1");
            HttpServletResponse response = mock(HttpServletResponse.class);
            FilterChain chain = mock(FilterChain.class);
            doThrow(new ServletException("test")).when(chain).doFilter(request, response);

            try {
                filter.doFilter(request, response, chain);
            } catch (ServletException e) {
                // expected
            }

            assertEquals(GrayConstants.TAG_STABLE, GrayContext.get());
        }
    }
}