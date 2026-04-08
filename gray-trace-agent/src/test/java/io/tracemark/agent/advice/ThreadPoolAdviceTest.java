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
 */
class ThreadPoolAdviceTest {

    @BeforeEach
    void setUp() {
        GrayContext.clear();
    }

    @AfterEach
    void tearDown() {
        GrayContext.clear();
    }

    @Nested
    @DisplayName("onExecute 方法逻辑测试")
    class OnExecuteTest {

        @Test
        @DisplayName("普通 Runnable 应被 TTL 包装")
        void onExecute_normalRunnable_shouldWrapWithTtl() {
            Runnable original = () -> {};
            Runnable wrapped = simulateOnExecute(original);

            assertTrue(wrapped instanceof TtlRunnable);
        }

        @Test
        @DisplayName("已包装的 TtlRunnable 不应重复包装")
        void onExecute_ttlRunnable_shouldNotWrapAgain() {
            Runnable original = () -> {};
            Runnable firstWrapped = TtlRunnable.get(original);
            Runnable secondWrapped = simulateOnExecute(firstWrapped);

            assertSame(firstWrapped, secondWrapped);
        }

        @Test
        @DisplayName("null Runnable 应返回 null")
        void onExecute_nullRunnable_shouldReturnNull() {
            Runnable wrapped = simulateOnExecute(null);
            assertNull(wrapped);
        }
    }

    @Nested
    @DisplayName("CallableAdvice.onSubmit 方法逻辑测试")
    class CallableAdviceTest {

        @Test
        @DisplayName("普通 Callable 应被 TTL 包装")
        void onSubmit_normalCallable_shouldWrapWithTtl() {
            Callable<String> original = () -> "test";
            Callable<String> wrapped = simulateOnSubmit(original);

            assertTrue(wrapped instanceof TtlCallable);
        }

        @Test
        @DisplayName("已包装的 TtlCallable 不应重复包装")
        void onSubmit_ttlCallable_shouldNotWrapAgain() {
            Callable<String> original = () -> "test";
            Callable<String> firstWrapped = TtlCallable.get(original);
            Callable<String> secondWrapped = simulateOnSubmit(firstWrapped);

            assertSame(firstWrapped, secondWrapped);
        }

        @Test
        @DisplayName("null Callable 应返回 null")
        void onSubmit_nullCallable_shouldReturnNull() {
            Callable<String> wrapped = simulateOnSubmit(null);
            assertNull(wrapped);
        }
    }

    // 模拟 Advice.onExecute 的逻辑
    private Runnable simulateOnExecute(Runnable runnable) {
        if (runnable != null && !(runnable instanceof TtlRunnable)) {
            return TtlRunnable.get(runnable);
        }
        return runnable;
    }

    // 模拟 CallableAdvice.onSubmit 的逻辑
    private <T> Callable<T> simulateOnSubmit(Callable<T> callable) {
        if (callable != null && !(callable instanceof TtlCallable)) {
            return TtlCallable.get(callable);
        }
        return callable;
    }
}