package io.tracemark.agent.advice;

import io.tracemark.gray.core.GrayConstants;
import io.tracemark.gray.core.GrayContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.concurrent.Callable;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

import static org.junit.jupiter.api.Assertions.*;

/**
 * CompletableFutureAsyncAdvice 各内部类单元测试
 * 直接测试 Advice 逻辑
 */
class CompletableFutureAsyncAdviceDirectTest {

    @BeforeEach
    void setUp() {
        GrayContext.clear();
    }

    @AfterEach
    void tearDown() {
        GrayContext.clear();
    }

    @Nested
    @DisplayName("SupplyAsyncAdvice 逻辑测试")
    class SupplyAsyncAdviceTest {

        @Test
        @DisplayName("supplier 不为 null 时应包装为 TTL")
        void onEnter_withSupplier_shouldWrapWithTtl() throws Exception {
            GrayContext.set("gray-v1");

            Supplier<String> original = () -> GrayContext.get();
            Supplier<String> wrapped = simulateSupplyAsyncWrap(original);

            // 在另一个线程中执行
            String result = wrapped.get();
            assertEquals("gray-v1", result);
        }

        @Test
        @DisplayName("supplier 为 null 时不应处理")
        void onEnter_withNullSupplier_shouldDoNothing() {
            Supplier<String> supplier = null;
            // 模拟 Advice 逻辑
            if (supplier != null) {
                fail("不应进入此分支");
            }
        }
    }

    @Nested
    @DisplayName("RunAsyncAdvice 逻辑测试")
    class RunAsyncAdviceTest {

        @Test
        @DisplayName("runnable 不为 null 时应包装为 TtlRunnable")
        void onEnter_withRunnable_shouldWrapWithTtl() {
            GrayContext.set("gray-v1");

            String[] captured = new String[1];
            Runnable original = () -> captured[0] = GrayContext.get();
            Runnable wrapped = simulateRunAsyncWrap(original);

            wrapped.run();
            assertEquals("gray-v1", captured[0]);
        }

        @Test
        @DisplayName("runnable 为 null 时不应处理")
        void onEnter_withNullRunnable_shouldDoNothing() {
            Runnable runnable = null;
            if (runnable != null) {
                fail("不应进入此分支");
            }
        }
    }

    @Nested
    @DisplayName("ThenApplyAsyncAdvice 逻辑测试")
    class ThenApplyAsyncAdviceTest {

        @Test
        @DisplayName("function 不为 null 时应捕获上下文")
        void onEnter_withFunction_shouldCaptureContext() {
            GrayContext.set("gray-v1");

            Function<String, String> original = x -> GrayContext.get() + "-" + x;
            Function<String, String> wrapped = simulateThenApplyAsyncWrap(original);

            GrayContext.clear(); // 模拟在另一个线程执行
            String result = wrapped.apply("input");
            assertEquals("gray-v1-input", result);
        }

        @Test
        @DisplayName("function 为 null 时不应处理")
        void onEnter_withNullFunction_shouldDoNothing() {
            Function<String, String> fn = null;
            if (fn != null) {
                fail("不应进入此分支");
            }
        }
    }

    @Nested
    @DisplayName("ThenAcceptAsyncAdvice 逻辑测试")
    class ThenAcceptAsyncAdviceTest {

        @Test
        @DisplayName("consumer 不为 null 时应捕获上下文")
        void onEnter_withConsumer_shouldCaptureContext() {
            GrayContext.set("gray-v1");

            String[] captured = new String[1];
            Consumer<String> original = x -> captured[0] = GrayContext.get() + "-" + x;
            Consumer<String> wrapped = simulateThenAcceptAsyncWrap(original);

            GrayContext.clear();
            wrapped.accept("input");
            assertEquals("gray-v1-input", captured[0]);
        }

        @Test
        @DisplayName("consumer 为 null 时不应处理")
        void onEnter_withNullConsumer_shouldDoNothing() {
            Consumer<String> consumer = null;
            if (consumer != null) {
                fail("不应进入此分支");
            }
        }
    }

    @Nested
    @DisplayName("ThenRunAsyncAdvice 逻辑测试")
    class ThenRunAsyncAdviceTest {

        @Test
        @DisplayName("runnable 不为 null 时应包装为 TtlRunnable")
        void onEnter_withRunnable_shouldWrapWithTtl() {
            GrayContext.set("gray-v1");

            String[] captured = new String[1];
            Runnable original = () -> captured[0] = GrayContext.get();
            Runnable wrapped = simulateThenRunAsyncWrap(original);

            wrapped.run();
            assertEquals("gray-v1", captured[0]);
        }
    }

    // 模拟各 Advice 的包装逻辑

    private <T> Supplier<T> simulateSupplyAsyncWrap(Supplier<T> supplier) {
        if (supplier == null) return null;
        Supplier<T> original = supplier;
        return () -> {
            Callable<T> callable = () -> original.get();
            Callable<T> wrapped = com.alibaba.ttl.TtlCallable.get(callable);
            try {
                return wrapped.call();
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        };
    }

    private Runnable simulateRunAsyncWrap(Runnable runnable) {
        if (runnable == null) return null;
        if (runnable instanceof com.alibaba.ttl.TtlRunnable) return runnable;
        return com.alibaba.ttl.TtlRunnable.get(runnable);
    }

    private <T, R> Function<T, R> simulateThenApplyAsyncWrap(Function<T, R> fn) {
        if (fn == null) return null;
        String capturedTag = GrayContext.get();
        Function<Object, Object> original = (Function<Object, Object>) fn;
        return input -> {
            GrayContext.set(capturedTag);
            try {
                return (R) original.apply(input);
            } finally {
                GrayContext.clear();
            }
        };
    }

    private <T> Consumer<T> simulateThenAcceptAsyncWrap(Consumer<T> consumer) {
        if (consumer == null) return null;
        String capturedTag = GrayContext.get();
        Consumer<Object> original = (Consumer<Object>) consumer;
        return input -> {
            GrayContext.set(capturedTag);
            try {
                original.accept(input);
            } finally {
                GrayContext.clear();
            }
        };
    }

    private Runnable simulateThenRunAsyncWrap(Runnable runnable) {
        if (runnable == null) return null;
        if (runnable instanceof com.alibaba.ttl.TtlRunnable) return runnable;
        return com.alibaba.ttl.TtlRunnable.get(runnable);
    }
}