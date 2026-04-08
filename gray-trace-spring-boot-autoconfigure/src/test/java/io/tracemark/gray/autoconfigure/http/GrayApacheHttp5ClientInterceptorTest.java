package io.tracemark.gray.autoconfigure.http;

import io.tracemark.gray.core.GrayConstants;
import io.tracemark.gray.core.GrayContext;
import org.apache.hc.core5.http.EntityDetails;
import org.apache.hc.core5.http.HttpRequest;
import org.apache.hc.core5.http.protocol.HttpContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.mockito.Mockito.*;

/**
 * GrayApacheHttp5ClientInterceptor 单元测试
 */
class GrayApacheHttp5ClientInterceptorTest {

    private GrayApacheHttp5ClientInterceptor interceptor;

    @BeforeEach
    void setUp() {
        interceptor = new GrayApacheHttp5ClientInterceptor();
        GrayContext.clear();
    }

    @AfterEach
    void tearDown() {
        GrayContext.clear();
    }

    @Nested
    @DisplayName("process 方法测试")
    class ProcessTest {

        @Test
        @DisplayName("灰度标存在时应注入 Header")
        void process_withGrayTag_shouldInjectHeader() {
            GrayContext.set("gray-v1");

            HttpRequest request = mock(HttpRequest.class);
            EntityDetails entity = mock(EntityDetails.class);
            HttpContext context = mock(HttpContext.class);

            interceptor.process(request, entity, context);

            verify(request).addHeader(GrayConstants.HEADER_GRAY_TAG, "gray-v1");
        }

        @Test
        @DisplayName("灰度标为 stable 时仍应注入 Header")
        void process_withStableTag_shouldInjectHeader() {
            GrayContext.set(GrayConstants.TAG_STABLE);

            HttpRequest request = mock(HttpRequest.class);
            EntityDetails entity = mock(EntityDetails.class);
            HttpContext context = mock(HttpContext.class);

            interceptor.process(request, entity, context);

            verify(request).addHeader(GrayConstants.HEADER_GRAY_TAG, GrayConstants.TAG_STABLE);
        }

        @Test
        @DisplayName("无灰度标时应注入 stable Header")
        void process_withoutTag_shouldInjectStableHeader() {
            GrayContext.clear();

            HttpRequest request = mock(HttpRequest.class);
            EntityDetails entity = mock(EntityDetails.class);
            HttpContext context = mock(HttpContext.class);

            interceptor.process(request, entity, context);

            // GrayContext.get() 默认返回 stable
            verify(request).addHeader(GrayConstants.HEADER_GRAY_TAG, GrayConstants.TAG_STABLE);
        }
    }
}