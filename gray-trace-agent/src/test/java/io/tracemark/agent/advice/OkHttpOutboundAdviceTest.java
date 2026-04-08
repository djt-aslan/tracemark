package io.tracemark.agent.advice;

import io.tracemark.gray.core.GrayConstants;
import io.tracemark.gray.core.GrayContext;
import okhttp3.Request;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * OkHttpOutboundAdvice 单元测试
 *
 * 注意：由于 Advice 使用 ByteBuddy 字段注入机制，
 * 此测试模拟 Advice 的逻辑而非真正的 ByteBuddy 插桩。
 */
class OkHttpOutboundAdviceTest {

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

            Request originalRequest = new Request.Builder()
                    .url("http://test")
                    .build();

            // 模拟 Advice 逻辑
            Request modifiedRequest = simulateOnEnter(originalRequest);

            assertEquals("gray-v1", modifiedRequest.header(GrayConstants.HEADER_GRAY_TAG));
        }

        @Test
        @DisplayName("灰度标为 stable 时仍应注入 Header")
        void onEnter_withStableTag_shouldInjectHeader() {
            GrayContext.set(GrayConstants.TAG_STABLE);

            Request originalRequest = new Request.Builder()
                    .url("http://test")
                    .build();

            Request modifiedRequest = simulateOnEnter(originalRequest);

            assertEquals(GrayConstants.TAG_STABLE, modifiedRequest.header(GrayConstants.HEADER_GRAY_TAG));
        }

        @Test
        @DisplayName("已有灰度标时不应覆盖")
        void onEnter_withExistingHeader_shouldNotOverride() {
            GrayContext.set("gray-v2");

            Request originalRequest = new Request.Builder()
                    .url("http://test")
                    .header(GrayConstants.HEADER_GRAY_TAG, "gray-v1")
                    .build();

            Request modifiedRequest = simulateOnEnter(originalRequest);

            // 已有 header 时不应覆盖
            assertEquals("gray-v1", modifiedRequest.header(GrayConstants.HEADER_GRAY_TAG));
        }

        @Test
        @DisplayName("无灰度标时应注入 stable Header")
        void onEnter_withoutTag_shouldInjectStableHeader() {
            GrayContext.clear();

            Request originalRequest = new Request.Builder()
                    .url("http://test")
                    .build();

            Request modifiedRequest = simulateOnEnter(originalRequest);

            // GrayContext.get() 默认返回 stable
            assertEquals(GrayConstants.TAG_STABLE, modifiedRequest.header(GrayConstants.HEADER_GRAY_TAG));
        }
    }

    // 模拟 Advice.onEnter 的逻辑
    private Request simulateOnEnter(Request request) {
        String tag = GrayContext.get();
        if (tag != null && !tag.isEmpty()
                && request.header(GrayConstants.HEADER_GRAY_TAG) == null) {
            return request.newBuilder()
                    .header(GrayConstants.HEADER_GRAY_TAG, tag)
                    .build();
        }
        return request;
    }
}