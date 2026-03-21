package io.tracemark.test.controller;

import io.tracemark.gray.core.GrayContext;
import io.tracemark.test.service.AsyncService;
import io.tracemark.test.service.HttpClientService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

/**
 * 灰度追踪验证接口
 *
 * <p>通过携带 {@code x-gray-tag: gray-v1} Header 调用各接口，
 * 观察响应中每个场景是否都正确输出了灰度标。
 *
 * <pre>
 * # 灰度请求
 * curl -H "x-gray-tag: gray-v1" http://localhost:8080/gray/all
 *
 * # 稳定请求（不带 Header）
 * curl http://localhost:8080/gray/all
 * </pre>
 */
@RestController
@RequestMapping("/gray")
public class GrayVerifyController {

    private final AsyncService asyncService;
    private final HttpClientService httpClientService;

    public GrayVerifyController(AsyncService asyncService, HttpClientService httpClientService) {
        this.asyncService = asyncService;
        this.httpClientService = httpClientService;
    }

    /**
     * 一次性验证所有场景
     */
    @GetMapping("/all")
    public Map<String, Object> verifyAll() throws ExecutionException, InterruptedException {
        Map<String, Object> result = new LinkedHashMap<>();

        // 1. 当前线程（Servlet 入口提取）
        result.put("1_servlet_main_thread", GrayContext.get());

        // 2. @Async 线程（Spring 管理的异步线程池）
        CompletableFuture<String> asyncResult = asyncService.asyncMethod();
        result.put("2_at_async", asyncResult.get());

        // 3. 自定义 ThreadPoolExecutor
        CompletableFuture<String> poolResult = asyncService.threadPoolExecutor();
        result.put("3_thread_pool_executor", poolResult.get());

        // 4. CompletableFuture.supplyAsync（公共 ForkJoinPool，TTL Agent 模式才能覆盖）
        String capturedTag = GrayContext.get();
        CompletableFuture<String> cfResult = CompletableFuture.supplyAsync(() -> {
            // Starter 模式下需手动传：在提交处捕获，在执行处使用
            return "captured_before_submit=" + capturedTag
                    + ", in_thread=" + GrayContext.get();
        });
        result.put("4_completable_future", cfResult.get());

        // 5. RestTemplate 出口（echo 目标：httpbin）
        result.put("5_rest_template_outbound", httpClientService.callViaRestTemplate());

        // 6. OkHttp 出口
        result.put("6_okhttp_outbound", httpClientService.callViaOkHttp());

        return result;
    }

    /** 单独验证 Servlet 入口提取 */
    @GetMapping("/servlet")
    public Map<String, String> verifyServlet() {
        return Map.of(
                "thread", Thread.currentThread().getName(),
                "gray_tag", GrayContext.get()
        );
    }

    /** 单独验证 @Async */
    @GetMapping("/async")
    public Map<String, Object> verifyAsync() throws ExecutionException, InterruptedException {
        return Map.of(
                "main_thread_tag", GrayContext.get(),
                "async_thread_tag", asyncService.asyncMethod().get()
        );
    }

    /** 单独验证线程池 */
    @GetMapping("/threadpool")
    public Map<String, Object> verifyThreadPool() throws ExecutionException, InterruptedException {
        return Map.of(
                "main_thread_tag", GrayContext.get(),
                "pool_thread_tag", asyncService.threadPoolExecutor().get()
        );
    }

    /** 单独验证 HTTP 出口 */
    @GetMapping("/http")
    public Map<String, Object> verifyHttp() {
        return Map.of(
                "main_thread_tag", GrayContext.get(),
                "rest_template", httpClientService.callViaRestTemplate(),
                "okhttp", httpClientService.callViaOkHttp()
        );
    }
}