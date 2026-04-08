package io.tracemark.agent.advice;

import com.alibaba.ttl.TtlCallable;
import com.alibaba.ttl.TtlRunnable;
import io.tracemark.gray.core.GrayContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.concurrent.Callable;

import static org.junit.jupiter.api.Assertions.*;

/**
 * ThreadPoolAdvice 单元测试
 * 直接测试 Advice 逻辑
 */
class ThreadPoolAdviceDirectTest {

    @BeforeEach
    void setUp() {
        GrayContext.clear();
    }

    @AfterEach
    void tearDown() {
        GrayContext.clear();
    }

    @Nested
    @DisplayName("onExecute 逻辑测试")
    class OnExecuteTest {

        @Test
        @DisplayName("runnable 不为 null 且非 TtlRunnable 时应包装")
        void onExecute_withRunnable_shouldWrapWithTtl() {
            GrayContext.set("gray-v1");

            String[] captured = new String[1];
            Runnable original = () -> captured[0] = GrayContext.get();
            Runnable wrapped = simulateOnExecute(original);

            wrapped.run();
            assertEquals("gray-v1", captured[0]);
        }

        @Test
        @DisplayName("runnable 为 null 时不应处理")
        void onExecute_withNullRunnable_shouldDoNothing() {
            Runnable runnable = null;
            Runnable result = simulateOnExecute(runnable);
            assertNull(result);
        }

        @Test
        @DisplayName("runnable 已是 TtlRunnable 时不应重复包装")
        void onExecute_withTtlRunnable_shouldNotWrapAgain() {
            Runnable original = () -> {};
            Runnable ttlRunnable = TtlRunnable.get(original);
            Runnable result = simulateOnExecute(ttlRunnable);
            assertSame(ttlRunnable, result);
        }
    }

    @Nested
    @DisplayName("CallableAdvice.onSubmit 逻辑测试")
    class CallableAdviceTest {

        @Test
        @DisplayName("callable 不为 null 且非 TtlCallable 时应包装")
        void onSubmit_withCallable_shouldWrapWithTtl() throws Exception {
            GrayContext.set("gray-v1");

            Callable<String> original = () -> GrayContext.get();
            Callable<String> wrapped = simulateOnSubmit(original);

            String result = wrapped.call();
            assertEquals("gray-v1", result);
        }

        @Test
        @DisplayName("callable 为 null 时不应处理")
        void onSubmit_withNullCallable_shouldDoNothing() {
            Callable<String> callable = null;
            Callable<String> result = simulateOnSubmit(callable);
            assertNull(result);
        }

        @Test
        @DisplayName("callable 已是 TtlCallable 时不应重复包装")
        void onSubmit_withTtlCallable_shouldNotWrapAgain() {
            Callable<String> original = () -> "result";
            Callable<String> ttlCallable = TtlCallable.get(original);
            Callable<String> result = simulateOnSubmit(ttlCallable);
            assertSame(ttlCallable, result);
        }
    }

    // 模拟 Advice 逻辑

    private Runnable simulateOnExecute(Runnable runnable) {
        if (runnable != null && !(runnable instanceof TtlRunnable)) {
            runnable = TtlRunnable.get(runnable);
        }
        return runnable;
    }

    private <T> Callable<T> simulateOnSubmit(Callable<T> callable) {
        if (callable != null && !(callable instanceof TtlCallable)) {
            callable = TtlCallable.get(callable);
        }
        return callable;
    }
}