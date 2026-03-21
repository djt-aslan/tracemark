package io.tracemark.gray.autoconfigure.http;

import io.tracemark.gray.core.GrayConstants;
import io.tracemark.gray.core.GrayContext;
import org.springframework.web.reactive.function.client.ClientRequest;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.ExchangeFilterFunction;
import org.springframework.web.reactive.function.client.ExchangeFunction;
import reactor.core.publisher.Mono;

/**
 * WebFlux WebClient 出口灰度头传递 Filter
 *
 * <p>在 Reactor 异步流中通过 {@code subscriberContext} 传递灰度标，
 * 同时兼容通过 {@link GrayContext} ThreadLocal 传递的场景。
 */
public class GrayWebClientFilter implements ExchangeFilterFunction {

    @Override
    public Mono<ClientResponse> filter(ClientRequest request, ExchangeFunction next) {
        return Mono.deferContextual(ctx -> {
            // 优先从 Reactor Context 取（适配 WebFlux 场景）
            String tag = ctx.getOrDefault(GrayConstants.HEADER_GRAY_TAG, null);
            // fallback：从 ThreadLocal 取（适配 MVC 中调用 WebClient 的场景）
            if (tag == null || tag.isEmpty()) {
                tag = GrayContext.get();
            }

            if (tag == null || tag.isEmpty()) {
                return next.exchange(request);
            }

            final String finalTag = tag;
            ClientRequest newRequest = ClientRequest.from(request)
                    .header(GrayConstants.HEADER_GRAY_TAG, finalTag)
                    .build();
            return next.exchange(newRequest);
        });
    }

    /** 工厂方法，便于链式注册 */
    public static GrayWebClientFilter instance() {
        return new GrayWebClientFilter();
    }
}