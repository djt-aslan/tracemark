package io.tracemark.gray.autoconfigure.http;

import io.tracemark.gray.core.GrayConstants;
import io.tracemark.gray.core.GrayContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.web.reactive.function.client.ClientRequest;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.ExchangeFunction;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.net.URI;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * GrayWebClientFilter 单元测试
 */
class GrayWebClientFilterTest {

    private GrayWebClientFilter filter;

    @BeforeEach
    void setUp() {
        filter = new GrayWebClientFilter();
        GrayContext.clear();
    }

    @AfterEach
    void tearDown() {
        GrayContext.clear();
    }

    @Nested
    @DisplayName("filter 方法测试")
    class FilterTest {

        @Test
        @DisplayName("灰度标存在时应注入 Header")
        void filter_withGrayTag_shouldInjectHeader() {
            GrayContext.set("gray-v1");

            ClientRequest request = ClientRequest.create(HttpMethod.GET, URI.create("http://test/api"))
                    .build();

            // Mock ExchangeFunction
            ClientResponse mockResponse = mock(ClientResponse.class);
            ExchangeFunction exchangeFunction = mock(ExchangeFunction.class);

            // 捕获修改后的请求
            when(exchangeFunction.exchange(any())).thenAnswer(invocation -> {
                ClientRequest capturedRequest = invocation.getArgument(0);
                assertEquals("gray-v1", capturedRequest.headers().getFirst(GrayConstants.HEADER_GRAY_TAG));
                return Mono.just(mockResponse);
            });

            StepVerifier.create(filter.filter(request, exchangeFunction))
                    .expectNext(mockResponse)
                    .verifyComplete();
        }

        @Test
        @DisplayName("灰度标为 stable 时应注入 Header")
        void filter_withStableTag_shouldInjectHeader() {
            GrayContext.set(GrayConstants.TAG_STABLE);

            ClientRequest request = ClientRequest.create(HttpMethod.GET, URI.create("http://test/api"))
                    .build();

            ClientResponse mockResponse = mock(ClientResponse.class);
            ExchangeFunction exchangeFunction = mock(ExchangeFunction.class);

            when(exchangeFunction.exchange(any())).thenAnswer(invocation -> {
                ClientRequest capturedRequest = invocation.getArgument(0);
                assertEquals(GrayConstants.TAG_STABLE, capturedRequest.headers().getFirst(GrayConstants.HEADER_GRAY_TAG));
                return Mono.just(mockResponse);
            });

            StepVerifier.create(filter.filter(request, exchangeFunction))
                    .expectNext(mockResponse)
                    .verifyComplete();
        }

        @Test
        @DisplayName("无灰度标时应注入 stable Header")
        void filter_withoutTag_shouldInjectStableHeader() {
            GrayContext.clear();

            ClientRequest request = ClientRequest.create(HttpMethod.GET, URI.create("http://test/api"))
                    .build();

            ClientResponse mockResponse = mock(ClientResponse.class);
            ExchangeFunction exchangeFunction = mock(ExchangeFunction.class);

            when(exchangeFunction.exchange(any())).thenAnswer(invocation -> {
                ClientRequest capturedRequest = invocation.getArgument(0);
                assertEquals(GrayConstants.TAG_STABLE, capturedRequest.headers().getFirst(GrayConstants.HEADER_GRAY_TAG));
                return Mono.just(mockResponse);
            });

            StepVerifier.create(filter.filter(request, exchangeFunction))
                    .expectNext(mockResponse)
                    .verifyComplete();
        }
    }

    @Nested
    @DisplayName("instance 工厂方法测试")
    class InstanceTest {

        @Test
        @DisplayName("instance 应返回新实例")
        void instance_shouldReturnNewInstance() {
            GrayWebClientFilter instance1 = GrayWebClientFilter.instance();
            GrayWebClientFilter instance2 = GrayWebClientFilter.instance();

            assertNotNull(instance1);
            assertNotNull(instance2);
            assertNotSame(instance1, instance2);
        }
    }
}