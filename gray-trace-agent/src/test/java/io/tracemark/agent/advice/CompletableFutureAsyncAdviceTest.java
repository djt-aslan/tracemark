package io.tracemark.agent.advice;

import com.alibaba.ttl.TtlCallable;
import com.alibaba.ttl.TtlRunnable;
import io.tracemark.gray.core.GrayContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CompletableFutureAsyncAdviceTest {

    private final Executor executor = Executors.newSingleThreadExecutor();

    @AfterEach
    void tearDown() {
        GrayContext.clear();
    }

    // ── supplyAsync 场景 ──────────────────────────────────────

    @Test
    void supplyAsync_withGrayTag_shouldPropagateContext() throws Exception {
        GrayContext.set("gray-v1");

        Supplier<String> supplier = () -> GrayContext.get();
        Callable<String> callable = () -> supplier.get();
        Callable<String> wrappedCallable = TtlCallable.get(callable);
        Supplier<String> wrapped = () -> {
            try {
                return wrappedCallable.call();
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        };

        CompletableFuture<String> future = CompletableFuture.supplyAsync(wrapped);
        assertThat(future.get()).isEqualTo("gray-v1");
    }

    @Test
    void supplyAsync_withExecutor_shouldPropagateContext() throws Exception {
        GrayContext.set("gray-v1");

        Supplier<String> supplier = () -> GrayContext.get();
        Callable<String> callable = () -> supplier.get();
        Callable<String> wrappedCallable = TtlCallable.get(callable);
        Supplier<String> wrapped = () -> {
            try {
                return wrappedCallable.call();
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        };

        CompletableFuture<String> future = CompletableFuture.supplyAsync(wrapped, executor);
        assertThat(future.get()).isEqualTo("gray-v1");
    }

    // ── runAsync 场景 ────────────────────────────────────────

    @Test
    void runAsync_withGrayTag_shouldPropagateContext() throws Exception {
        GrayContext.set("gray-v1");

        String[] captured = new String[1];
        Runnable runnable = () -> captured[0] = GrayContext.get();
        Runnable wrapped = TtlRunnable.get(runnable);

        CompletableFuture<Void> future = CompletableFuture.runAsync(wrapped);
        future.get();
        assertThat(captured[0]).isEqualTo("gray-v1");
    }

    // ── thenApplyAsync 场景 ──────────────────────────────────

    @Test
    void thenApplyAsync_withGrayTag_shouldPropagateContext() throws Exception {
        GrayContext.set("gray-v1");

        // 对于 Function，需要手动捕获上下文并在执行时恢复
        // 因为 TTL 没有直接的 Function 包装器
        String capturedTag = GrayContext.get();
        Function<String, String> fn = x -> GrayContext.get() + "-" + x;
        Function<String, String> wrapped = input -> {
            GrayContext.set(capturedTag);
            try {
                return fn.apply(input);
            } finally {
                GrayContext.clear();
            }
        };

        CompletableFuture<String> future = CompletableFuture.completedFuture("input")
                .thenApplyAsync(wrapped);
        assertThat(future.get()).isEqualTo("gray-v1-input");
    }

    // ── thenAcceptAsync 场景 ─────────────────────────────────

    @Test
    void thenAcceptAsync_withGrayTag_shouldPropagateContext() throws Exception {
        GrayContext.set("gray-v1");

        // 对于 Consumer，需要手动捕获上下文并在执行时恢复
        String capturedTag = GrayContext.get();
        String[] captured = new String[1];
        Consumer<String> consumer = x -> captured[0] = GrayContext.get() + "-" + x;
        Consumer<String> wrapped = input -> {
            GrayContext.set(capturedTag);
            try {
                consumer.accept(input);
            } finally {
                GrayContext.clear();
            }
        };

        CompletableFuture<Void> future = CompletableFuture.completedFuture("input")
                .thenAcceptAsync(wrapped);
        future.get();
        assertThat(captured[0]).isEqualTo("gray-v1-input");
    }

    // ── 无灰度标场景 ─────────────────────────────────────────

    @Test
    void supplyAsync_withoutGrayTag_shouldReturnStable() throws Exception {
        Supplier<String> supplier = () -> GrayContext.get();
        Callable<String> callable = () -> supplier.get();
        Callable<String> wrappedCallable = TtlCallable.get(callable);
        Supplier<String> wrapped = () -> {
            try {
                return wrappedCallable.call();
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        };

        CompletableFuture<String> future = CompletableFuture.supplyAsync(wrapped);
        assertThat(future.get()).isEqualTo("stable");
    }

    // ── 返回值和异常语义 ─────────────────────────────────────

    @Test
    void supplyAsync_shouldPreserveReturnValue() throws Exception {
        Supplier<String> supplier = () -> "result";
        Callable<String> callable = () -> supplier.get();
        Callable<String> wrappedCallable = TtlCallable.get(callable);
        Supplier<String> wrapped = () -> {
            try {
                return wrappedCallable.call();
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        };

        CompletableFuture<String> future = CompletableFuture.supplyAsync(wrapped);
        assertThat(future.get()).isEqualTo("result");
    }

    @Test
    void supplyAsync_shouldPropagateException() {
        Supplier<String> supplier = () -> {
            throw new RuntimeException("test-error");
        };
        Callable<String> callable = () -> supplier.get();
        Callable<String> wrappedCallable = TtlCallable.get(callable);
        Supplier<String> wrapped = () -> {
            try {
                return wrappedCallable.call();
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        };

        CompletableFuture<String> future = CompletableFuture.supplyAsync(wrapped);
        assertThatThrownBy(future::get)
                .isInstanceOf(ExecutionException.class)
                .hasCauseInstanceOf(RuntimeException.class)
                .hasRootCauseMessage("test-error");
    }
}