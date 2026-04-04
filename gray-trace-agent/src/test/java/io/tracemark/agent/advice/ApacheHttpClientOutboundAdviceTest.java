package io.tracemark.agent.advice;

import io.tracemark.gray.core.GrayConstants;
import io.tracemark.gray.core.GrayContext;
import org.apache.http.HttpHost;
import org.apache.http.message.BasicHttpRequest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

class ApacheHttpClientOutboundAdviceTest {

    @AfterEach
    void tearDown() {
        GrayContext.clear();
    }

    // ── 场景：有灰度标时注入 ──────────────────────────────────────

    @Test
    void onEnter_withGrayTag_shouldInjectHeader() {
        GrayContext.set("gray-v1");
        BasicHttpRequest request = new BasicHttpRequest("GET", "/api/test");

        ApacheHttpClientOutboundAdvice.onEnter(new Object[]{request});

        assertThat(request.getFirstHeader(GrayConstants.HEADER_GRAY_TAG)).isNotNull();
        assertThat(request.getFirstHeader(GrayConstants.HEADER_GRAY_TAG).getValue())
                .isEqualTo("gray-v1");
    }

    // ── 场景：stable 标不注入 ─────────────────────────────────────

    @Test
    void onEnter_withStableTag_shouldNotInjectHeader() {
        // GrayContext 默认值为 "stable"，isGray() == false
        BasicHttpRequest request = new BasicHttpRequest("GET", "/api/test");

        ApacheHttpClientOutboundAdvice.onEnter(new Object[]{request});

        assertThat(request.getFirstHeader(GrayConstants.HEADER_GRAY_TAG)).isNull();
    }

    // ── 场景：Header 已存在时不覆盖 ──────────────────────────────

    @Test
    void onEnter_withExistingHeader_shouldNotOverwrite() {
        GrayContext.set("gray-v2");
        BasicHttpRequest request = new BasicHttpRequest("GET", "/api/test");
        request.setHeader(GrayConstants.HEADER_GRAY_TAG, "gray-v1");

        ApacheHttpClientOutboundAdvice.onEnter(new Object[]{request});

        assertThat(request.getFirstHeader(GrayConstants.HEADER_GRAY_TAG).getValue())
                .isEqualTo("gray-v1");  // 原值保持
    }

    // ── 场景：execute(HttpHost, HttpRequest) 重载 ─────────────────

    @Test
    void onEnter_withHostPlusRequestArgs_shouldInjectOnRequest() {
        GrayContext.set("gray-v1");
        BasicHttpRequest request = new BasicHttpRequest("GET", "/api/test");

        // 模拟 execute(HttpHost target, HttpRequest request) 签名
        ApacheHttpClientOutboundAdvice.onEnter(new Object[]{
                new HttpHost("example.com"),
                request
        });

        assertThat(request.getFirstHeader(GrayConstants.HEADER_GRAY_TAG).getValue())
                .isEqualTo("gray-v1");
    }

    // ── 场景：null / 无 HttpRequest 参数时静默跳过 ────────────────

    @Test
    void onEnter_withNullArgs_shouldNotThrow() {
        assertThatCode(() -> ApacheHttpClientOutboundAdvice.onEnter(null))
                .doesNotThrowAnyException();
    }

    @Test
    void onEnter_withNoHttpRequestInArgs_shouldNotThrow() {
        GrayContext.set("gray-v1");
        assertThatCode(() -> ApacheHttpClientOutboundAdvice.onEnter(new Object[]{"not-a-request", 42}))
                .doesNotThrowAnyException();
    }
}
