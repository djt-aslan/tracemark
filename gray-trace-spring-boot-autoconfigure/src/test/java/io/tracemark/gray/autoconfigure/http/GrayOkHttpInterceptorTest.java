package io.tracemark.gray.autoconfigure.http;

import io.tracemark.gray.core.GrayConstants;
import io.tracemark.gray.core.GrayContext;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.*;

/**
 * GrayOkHttpInterceptor 单元测试
 */
class GrayOkHttpInterceptorTest {

    private GrayOkHttpInterceptor interceptor;
    private MockWebServer server;

    @BeforeEach
    void setUp() throws IOException {
        interceptor = new GrayOkHttpInterceptor();
        server = new MockWebServer();
        server.start();
        GrayContext.clear();
    }

    @AfterEach
    void tearDown() throws IOException {
        GrayContext.clear();
        server.shutdown();
    }

    @Nested
    @DisplayName("intercept 方法测试")
    class InterceptTest {

        @Test
        @DisplayName("灰度标存在时应注入 Header")
        void intercept_withGrayTag_shouldInjectHeader() throws Exception {
            GrayContext.set("gray-v1");

            OkHttpClient client = new OkHttpClient.Builder()
                    .addInterceptor(interceptor)
                    .build();

            server.enqueue(new okhttp3.mockwebserver.MockResponse().setBody("OK"));

            Request request = new Request.Builder()
                    .url(server.url("/test"))
                    .build();

            try (Response response = client.newCall(request).execute()) {
                RecordedRequest recorded = server.takeRequest();
                assertEquals("gray-v1", recorded.getHeader(GrayConstants.HEADER_GRAY_TAG));
            }
        }

        @Test
        @DisplayName("灰度标为 stable 时应注入 Header")
        void intercept_withStableTag_shouldInjectHeader() throws Exception {
            GrayContext.set(GrayConstants.TAG_STABLE);

            OkHttpClient client = new OkHttpClient.Builder()
                    .addInterceptor(interceptor)
                    .build();

            server.enqueue(new okhttp3.mockwebserver.MockResponse().setBody("OK"));

            Request request = new Request.Builder()
                    .url(server.url("/test"))
                    .build();

            try (Response response = client.newCall(request).execute()) {
                RecordedRequest recorded = server.takeRequest();
                assertEquals(GrayConstants.TAG_STABLE, recorded.getHeader(GrayConstants.HEADER_GRAY_TAG));
            }
        }

        @Test
        @DisplayName("无灰度标时应注入 stable Header")
        void intercept_withoutTag_shouldInjectStableHeader() throws Exception {
            GrayContext.clear();

            OkHttpClient client = new OkHttpClient.Builder()
                    .addInterceptor(interceptor)
                    .build();

            server.enqueue(new okhttp3.mockwebserver.MockResponse().setBody("OK"));

            Request request = new Request.Builder()
                    .url(server.url("/test"))
                    .build();

            try (Response response = client.newCall(request).execute()) {
                RecordedRequest recorded = server.takeRequest();
                // GrayContext.get() 默认返回 stable
                assertEquals(GrayConstants.TAG_STABLE, recorded.getHeader(GrayConstants.HEADER_GRAY_TAG));
            }
        }
    }
}