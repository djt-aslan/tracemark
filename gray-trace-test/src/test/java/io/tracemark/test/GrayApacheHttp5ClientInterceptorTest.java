package io.tracemark.test;

import io.tracemark.gray.autoconfigure.http.GrayApacheHttp5ClientBeanPostProcessor;
import io.tracemark.gray.autoconfigure.http.GrayApacheHttp5ClientInterceptor;
import io.tracemark.gray.core.GrayConstants;
import io.tracemark.gray.core.GrayContext;
import io.tracemark.gray.core.GrayProperties;
import org.apache.hc.core5.http.ClassicHttpRequest;
import org.apache.hc.core5.http.message.BasicClassicHttpRequest;
import org.apache.hc.core5.http.protocol.BasicHttpContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Apache HttpClient 5.x 拦截器及 BeanPostProcessor 单元测试
 */
class GrayApacheHttp5ClientInterceptorTest {

    private final GrayApacheHttp5ClientInterceptor interceptor = new GrayApacheHttp5ClientInterceptor();

    @AfterEach
    void tearDown() {
        GrayContext.clear();
    }

    // ──────────────────────────────────────────────────────
    // 场景 6.5(a)：有灰度标签时请求头被正确注入
    // ──────────────────────────────────────────────────────

    @Test
    @DisplayName("场景6.5-有灰度上下文时应注入x-gray-tag请求头（5.x）")
    void process_withGrayContext_shouldInjectHeader() throws Exception {
        GrayContext.set("gray-v2");
        ClassicHttpRequest request = new BasicClassicHttpRequest("GET", "/api/test");

        interceptor.process(request, null, new BasicHttpContext());

        assertThat(request.getFirstHeader(GrayConstants.HEADER_GRAY_TAG)).isNotNull();
        assertThat(request.getFirstHeader(GrayConstants.HEADER_GRAY_TAG).getValue()).isEqualTo("gray-v2");
    }

    // ──────────────────────────────────────────────────────
    // 场景 6.5(b)：灰度标签为空时不注入请求头
    // ──────────────────────────────────────────────────────

    @Test
    @DisplayName("场景6.5-无灰度上下文时应注入stable标签（GrayContext默认值为stable）（5.x）")
    void process_withoutGrayContext_shouldInjectStableHeader() throws Exception {
        ClassicHttpRequest request = new BasicClassicHttpRequest("GET", "/api/test");

        interceptor.process(request, null, new BasicHttpContext());

        assertThat(request.getFirstHeader(GrayConstants.HEADER_GRAY_TAG)).isNotNull();
        assertThat(request.getFirstHeader(GrayConstants.HEADER_GRAY_TAG).getValue())
                .isEqualTo(GrayConstants.TAG_STABLE);
    }

    // ──────────────────────────────────────────────────────
    // 场景 6.5(c)：enabled=false 时 BeanPostProcessor 不注入拦截器
    // ──────────────────────────────────────────────────────

    @Test
    @DisplayName("场景6.5-apacheHttpClient.enabled=false时BeanPostProcessor应跳过注入（5.x）")
    void beanPostProcessor_whenDisabled_shouldNotWrapBean() throws Exception {
        GrayProperties properties = new GrayProperties();
        properties.getApacheHttpClient().setEnabled(false);

        GrayApacheHttp5ClientBeanPostProcessor processor =
                new GrayApacheHttp5ClientBeanPostProcessor(properties);

        org.apache.hc.client5.http.impl.classic.CloseableHttpClient original =
                org.apache.hc.client5.http.impl.classic.HttpClientBuilder.create().build();

        Object result = processor.postProcessAfterInitialization(original, "httpClient5");

        assertThat(result).isSameAs(original);
    }

    // ──────────────────────────────────────────────────────
    // 场景 6.5(d)：BeanPostProcessor 对非 CloseableHttpClient 类型跳过
    // ──────────────────────────────────────────────────────

    @Test
    @DisplayName("场景6.5-非CloseableHttpClient类型的Bean应原样返回（5.x）")
    void beanPostProcessor_withNonHttpClientBean_shouldReturnAsIs() {
        GrayProperties properties = new GrayProperties();
        GrayApacheHttp5ClientBeanPostProcessor processor =
                new GrayApacheHttp5ClientBeanPostProcessor(properties);

        Object nonHttpClientBean = new Object();
        Object result = processor.postProcessAfterInitialization(nonHttpClientBean, "someBean");

        assertThat(result).isSameAs(nonHttpClientBean);
    }
}
