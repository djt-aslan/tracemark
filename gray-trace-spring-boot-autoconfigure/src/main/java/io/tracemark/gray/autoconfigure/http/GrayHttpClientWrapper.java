package io.tracemark.gray.autoconfigure.http;

import io.tracemark.gray.core.GrayConstants;
import io.tracemark.gray.core.GrayContext;

import java.net.Authenticator;
import java.net.ProxySelector;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLParameters;

/**
 * JDK 11+ {@link HttpClient} 灰度头透传包装器
 *
 * <p>JDK HttpClient 不提供拦截器接口，此包装器代理所有 send/sendAsync 方法，
 * 自动注入 {@code x-gray-tag} Header。
 *
 * <p>Starter 模式下，通过 {@link GrayHttpClientBeanPostProcessor} 自动替换 Spring Bean。
 */
public class GrayHttpClientWrapper extends HttpClient {

    private final HttpClient delegate;

    private GrayHttpClientWrapper(HttpClient delegate) {
        this.delegate = delegate;
    }

    public static HttpClient wrap(HttpClient client) {
        if (client instanceof GrayHttpClientWrapper) {
            return client;
        }
        return new GrayHttpClientWrapper(client);
    }

    @Override
    public <T> HttpResponse<T> send(HttpRequest request, HttpResponse.BodyHandler<T> handler)
            throws java.io.IOException, InterruptedException {
        return delegate.send(injectGrayTag(request), handler);
    }

    @Override
    public <T> CompletableFuture<HttpResponse<T>> sendAsync(
            HttpRequest request, HttpResponse.BodyHandler<T> handler) {
        return delegate.sendAsync(injectGrayTag(request), handler);
    }

    @Override
    public <T> CompletableFuture<HttpResponse<T>> sendAsync(
            HttpRequest request, HttpResponse.BodyHandler<T> handler,
            HttpResponse.PushPromiseHandler<T> pushPromiseHandler) {
        return delegate.sendAsync(injectGrayTag(request), handler, pushPromiseHandler);
    }

    private HttpRequest injectGrayTag(HttpRequest request) {
        String tag = GrayContext.get();
        if (tag == null || tag.isEmpty()) {
            return request;
        }
        if (request.headers().firstValue(GrayConstants.HEADER_GRAY_TAG).isPresent()) {
            return request;
        }
        return HttpRequest.newBuilder(request, (name, value) -> true)
                .header(GrayConstants.HEADER_GRAY_TAG, tag)
                .build();
    }

    // ===== 代理 HttpClient 其余方法 =====

    @Override
    public Optional<Authenticator> authenticator() {
        return delegate.authenticator();
    }

    @Override
    public HttpClient.Version version() {
        return delegate.version();
    }

    @Override
    public Optional<ProxySelector> proxy() {
        return delegate.proxy();
    }

    @Override
    public SSLContext sslContext() {
        return delegate.sslContext();
    }

    @Override
    public SSLParameters sslParameters() {
        return delegate.sslParameters();
    }

    @Override
    public HttpClient.Redirect followRedirects() {
        return delegate.followRedirects();
    }

    @Override
    public Optional<Executor> executor() {
        return delegate.executor();
    }

    @Override
    public Optional<java.time.Duration> connectTimeout() {
        return delegate.connectTimeout();
    }

    @Override
    public Optional<java.net.CookieHandler> cookieHandler() {
        return delegate.cookieHandler();
    }
}