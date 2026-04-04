package io.tracemark.test;

import io.tracemark.gray.autoconfigure.http.GrayApacheHttpClientBeanPostProcessor;
import io.tracemark.gray.autoconfigure.http.GrayApacheHttpClientInterceptor;
import io.tracemark.gray.core.GrayConstants;
import io.tracemark.gray.core.GrayContext;
import io.tracemark.gray.core.GrayProperties;
import org.apache.http.HttpRequest;
import org.apache.http.message.BasicHttpRequest;
import org.apache.http.protocol.BasicHttpContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Apache HttpClient 4.x 拦截器及 BeanPostProcessor 单元测试
 */
class GrayApacheHttpClientInterceptorTest {

    private final GrayApacheHttpClientInterceptor interceptor = new GrayApacheHttpClientInterceptor();

    @AfterEach
    void tearDown() {
        GrayContext.clear();
    }

    // ──────────────────────────────────────────────────────
    // 场景 6.1：有灰度标签时请求头被正确注入
    // ──────────────────────────────────────────────────────

    @Test
    @DisplayName("场景6.1-有灰度上下文时应注入x-gray-tag请求头")
    void process_withGrayContext_shouldInjectHeader() throws Exception {
        GrayContext.set("gray-v1");
        HttpRequest request = new BasicHttpRequest("GET", "/api/test");

        interceptor.process(request, new BasicHttpContext());

        assertThat(request.getFirstHeader(GrayConstants.HEADER_GRAY_TAG)).isNotNull();
        assertThat(request.getFirstHeader(GrayConstants.HEADER_GRAY_TAG).getValue()).isEqualTo("gray-v1");
    }

    // ──────────────────────────────────────────────────────
    // 场景 6.2：灰度标签为空时不注入请求头
    // ──────────────────────────────────────────────────────

    @Test
    @DisplayName("场景6.2-无灰度上下文时应注入stable标签（GrayContext默认值为stable）")
    void process_withoutGrayContext_shouldInjectStableHeader() throws Exception {
        HttpRequest request = new BasicHttpRequest("GET", "/api/test");

        interceptor.process(request, new BasicHttpContext());

        assertThat(request.getFirstHeader(GrayConstants.HEADER_GRAY_TAG)).isNotNull();
        assertThat(request.getFirstHeader(GrayConstants.HEADER_GRAY_TAG).getValue())
                .isEqualTo(GrayConstants.TAG_STABLE);
    }

    @Test
    @DisplayName("场景6.2-set空字符串时GrayContext回退为stable，应注入stable标签")
    void process_withEmptyGrayContext_shouldInjectStableHeader() throws Exception {
        GrayContext.set("");
        HttpRequest request = new BasicHttpRequest("GET", "/api/test");

        interceptor.process(request, new BasicHttpContext());

        assertThat(request.getFirstHeader(GrayConstants.HEADER_GRAY_TAG)).isNotNull();
        assertThat(request.getFirstHeader(GrayConstants.HEADER_GRAY_TAG).getValue())
                .isEqualTo(GrayConstants.TAG_STABLE);
    }

    // ──────────────────────────────────────────────────────
    // 场景 6.3：enabled=false 时 BeanPostProcessor 不注入拦截器
    // ──────────────────────────────────────────────────────

    @Test
    @DisplayName("场景6.3-apacheHttpClient.enabled=false时BeanPostProcessor应跳过注入")
    void beanPostProcessor_whenDisabled_shouldNotWrapBean() throws Exception {
        GrayProperties properties = new GrayProperties();
        properties.getApacheHttpClient().setEnabled(false);

        GrayApacheHttpClientBeanPostProcessor processor =
                new GrayApacheHttpClientBeanPostProcessor(properties);

        org.apache.http.impl.client.CloseableHttpClient original =
                org.apache.http.impl.client.HttpClientBuilder.create().build();

        Object result = processor.postProcessAfterInitialization(original, "httpClient");

        assertThat(result).isSameAs(original);
    }

    @Test
    @DisplayName("场景6.3-全局enabled=false时BeanPostProcessor应跳过注入")
    void beanPostProcessor_whenGloballyDisabled_shouldNotWrapBean() throws Exception {
        GrayProperties properties = new GrayProperties();
        properties.setEnabled(false);

        GrayApacheHttpClientBeanPostProcessor processor =
                new GrayApacheHttpClientBeanPostProcessor(properties);

        org.apache.http.impl.client.CloseableHttpClient original =
                org.apache.http.impl.client.HttpClientBuilder.create().build();

        Object result = processor.postProcessAfterInitialization(original, "httpClient");

        assertThat(result).isSameAs(original);
    }

    // ──────────────────────────────────────────────────────
    // 场景 6.4：BeanPostProcessor 对非 CloseableHttpClient 类型跳过
    // ──────────────────────────────────────────────────────

    @Test
    @DisplayName("场景6.4-非CloseableHttpClient类型的Bean应原样返回")
    void beanPostProcessor_withNonHttpClientBean_shouldReturnAsIs() {
        GrayProperties properties = new GrayProperties();
        GrayApacheHttpClientBeanPostProcessor processor =
                new GrayApacheHttpClientBeanPostProcessor(properties);

        Object nonHttpClientBean = new Object();
        Object result = processor.postProcessAfterInitialization(nonHttpClientBean, "someBean");

        assertThat(result).isSameAs(nonHttpClientBean);
    }
}
