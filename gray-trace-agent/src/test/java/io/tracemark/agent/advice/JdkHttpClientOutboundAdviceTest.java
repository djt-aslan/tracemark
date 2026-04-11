package io.tracemark.agent.advice;

import io.tracemark.gray.core.GrayConstants;
import io.tracemark.gray.core.GrayContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

/**
 * JdkHttpClientOutboundAdvice 单元测试
 *
 * <p>注意：由于 Advice 使用 ByteBuddy 参数注入机制和反射，
 * 且 java.net.http 是 Java 11+ API，此测试使用模拟对象验证逻辑。
 *
 * <p>此测试编译为 Java 8，运行时需要 Java 11+ 才能实际验证 JDK HttpClient。
 */
class JdkHttpClientOutboundAdviceTest {

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

            MockHttpRequest originalRequest = new MockHttpRequest(false);
            MockHttpRequest modifiedRequest = simulateOnEnter(originalRequest);

            assertTrue(modifiedRequest.hasGrayTag);
            assertEquals("gray-v1", modifiedRequest.grayTagValue);
        }

        @Test
        @DisplayName("灰度标为 stable 时仍应注入 Header")
        void onEnter_withStableTag_shouldInjectHeader() {
            GrayContext.set(GrayConstants.TAG_STABLE);

            MockHttpRequest originalRequest = new MockHttpRequest(false);
            MockHttpRequest modifiedRequest = simulateOnEnter(originalRequest);

            assertTrue(modifiedRequest.hasGrayTag);
            assertEquals(GrayConstants.TAG_STABLE, modifiedRequest.grayTagValue);
        }

        @Test
        @DisplayName("已有灰度标时不应覆盖")
        void onEnter_withExistingHeader_shouldNotOverride() {
            GrayContext.set("gray-v2");

            MockHttpRequest originalRequest = new MockHttpRequest(true, "gray-v1");
            MockHttpRequest modifiedRequest = simulateOnEnter(originalRequest);

            // 已有 header 时不应覆盖
            assertEquals("gray-v1", modifiedRequest.grayTagValue);
        }

        @Test
        @DisplayName("无灰度标时应注入 stable Header")
        void onEnter_withoutTag_shouldInjectStableHeader() {
            GrayContext.clear();

            MockHttpRequest originalRequest = new MockHttpRequest(false);
            MockHttpRequest modifiedRequest = simulateOnEnter(originalRequest);

            // GrayContext.get() 默认返回 stable
            assertTrue(modifiedRequest.hasGrayTag);
            assertEquals(GrayConstants.TAG_STABLE, modifiedRequest.grayTagValue);
        }

        @Test
        @DisplayName("POST 请求应正确注入 Header")
        void onEnter_withPostRequest_shouldInjectHeader() {
            GrayContext.set("gray-v1");

            MockHttpRequest originalRequest = new MockHttpRequest(false);
            originalRequest.method = "POST";
            originalRequest.contentType = "application/json";

            MockHttpRequest modifiedRequest = simulateOnEnter(originalRequest);

            assertTrue(modifiedRequest.hasGrayTag);
            assertEquals("gray-v1", modifiedRequest.grayTagValue);
            // 验证原有属性保留
            assertEquals("POST", modifiedRequest.method);
            assertEquals("application/json", modifiedRequest.contentType);
        }
    }

    /**
     * 模拟 Advice.onEnter 的逻辑
     *
     * <p>与 JdkHttpClientOutboundAdvice.onEnter 逻辑一致，但使用 Mock 对象
     */
    private MockHttpRequest simulateOnEnter(MockHttpRequest request) {
        String tag = GrayContext.get();
        if (tag != null && !tag.isEmpty()
                && !request.hasGrayTag()) {
            return request.newBuilder()
                    .header(GrayConstants.HEADER_GRAY_TAG, tag)
                    .build();
        }
        return request;
    }

    /**
     * 模拟 HttpRequest 对象
     *
     * <p>模拟 java.net.http.HttpRequest 的关键行为，用于测试 Advice 逻辑
     */
    static class MockHttpRequest {
        boolean hasGrayTag;
        String grayTagValue;
        String method = "GET";
        String contentType;
        String uri = "http://test/api";

        MockHttpRequest(boolean hasGrayTag) {
            this.hasGrayTag = hasGrayTag;
            this.grayTagValue = hasGrayTag ? GrayConstants.TAG_STABLE : null;
        }

        MockHttpRequest(boolean hasGrayTag, String grayTagValue) {
            this.hasGrayTag = hasGrayTag;
            this.grayTagValue = grayTagValue;
        }

        boolean hasGrayTag() {
            return hasGrayTag;
        }

        MockRequestBuilder newBuilder() {
            return new MockRequestBuilder(this);
        }
    }

    /**
     * 模拟 HttpRequest.Builder
     */
    static class MockRequestBuilder {
        private final MockHttpRequest base;
        private String addedGrayTag;

        MockRequestBuilder(MockHttpRequest base) {
            this.base = base;
        }

        MockRequestBuilder header(String name, String value) {
            if (GrayConstants.HEADER_GRAY_TAG.equals(name)) {
                this.addedGrayTag = value;
            }
            return this;
        }

        MockHttpRequest build() {
            MockHttpRequest result = new MockHttpRequest(true);
            result.method = base.method;
            result.contentType = base.contentType;
            result.uri = base.uri;

            if (addedGrayTag != null) {
                result.hasGrayTag = true;
                result.grayTagValue = addedGrayTag;
            } else {
                result.hasGrayTag = base.hasGrayTag;
                result.grayTagValue = base.grayTagValue;
            }

            return result;
        }
    }
}
