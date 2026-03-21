package io.tracemark.test.service;

import io.tracemark.gray.core.GrayContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * 验证异步场景下的灰度上下文传递
 */
@Service
public class AsyncService {

    private static final Logger log = LoggerFactory.getLogger(AsyncService.class);

    /**
     * 自定义 ThreadPoolExecutor（非 Spring 管理，Agent 模式下通过 ByteBuddy 插桩传递）
     * Starter 模式下：通过 GrayExecutorBeanPostProcessor 包装为 TTL Executor（Bean需要注入）
     */
    private final Executor customPool = new ThreadPoolExecutor(
            2, 4, 60L, TimeUnit.SECONDS,
            new LinkedBlockingQueue<>(100),
            r -> new Thread(r, "gray-custom-pool")
    );

    /**
     * 场景 2：Spring @Async
     * Starter 模式：GrayExecutorBeanPostProcessor 为 ThreadPoolTaskExecutor 设置了 TaskDecorator
     */
    @Async
    public CompletableFuture<String> asyncMethod() {
        String tag = GrayContext.get();
        log.info("[AsyncService] @Async thread={}, grayTag={}", Thread.currentThread().getName(), tag);
        return CompletableFuture.completedFuture(
                "thread=" + Thread.currentThread().getName() + ", tag=" + tag
        );
    }

    /**
     * 场景 3：自定义 ThreadPoolExecutor
     * Agent 模式：ByteBuddy 插桩 execute() 用 TTL 包装 Runnable
     * Starter 模式：若 customPool 注册为 Bean，BeanPostProcessor 会用 TtlExecutors 包装
     */
    public CompletableFuture<String> threadPoolExecutor() {
        String capturedTag = GrayContext.get();  // 提交时手动捕获（Starter 模式兜底）
        CompletableFuture<String> future = new CompletableFuture<>();
        customPool.execute(() -> {
            // Agent 模式：TTL 自动恢复
            // Starter 模式（customPool 未注册为 Bean）：依赖上方手动捕获的 capturedTag
            String tagInThread = GrayContext.get();
            log.info("[AsyncService] ThreadPool thread={}, grayTag={}", Thread.currentThread().getName(), tagInThread);
            future.complete(
                    "thread=" + Thread.currentThread().getName()
                    + ", tag_in_thread=" + tagInThread
                    + ", captured_before_submit=" + capturedTag
            );
        });
        return future;
    }
}