package io.tracemark.agent.advice;

import com.alibaba.ttl.TtlCallable;
import com.alibaba.ttl.TtlRunnable;
import io.tracemark.agent.GrayTraceLogger;
import io.tracemark.gray.core.GrayContext;
import net.bytebuddy.asm.Advice;

import java.util.concurrent.Callable;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * Advice：拦截 CompletableFuture 异步方法，用 TTL 包装函数式接口参数
 *
 * <p>处理的异步方法：
 * <ul>
 *   <li>supplyAsync(Supplier) / supplyAsync(Supplier, Executor)</li>
 *   <li>runAsync(Runnable) / runAsync(Runnable, Executor)</li>
 *   <li>thenApplyAsync(Function) / thenApplyAsync(Function, Executor)</li>
 *   <li>thenAcceptAsync(Consumer) / thenAcceptAsync(Consumer, Executor)</li>
 *   <li>thenRunAsync(Runnable) / thenRunAsync(Runnable, Executor)</li>
 * </ul>
 *
 * <p>使用 TTL 包装参数，保证灰度上下文跨线程传递。
 */
public class CompletableFutureAsyncAdvice {

    // ── supplyAsync ────────────────────────────────────────

    public static class SupplyAsyncAdvice {
        @Advice.OnMethodEnter
        public static void onEnter(@Advice.Argument(value = 0, readOnly = false) Supplier<?> supplier) {
            if (supplier != null) {
                String tag = GrayContext.get();
                GrayTraceLogger.logAsync(tag, "CompletableFuture", "Supplier", Thread.currentThread().getName());
                // Supplier 需要转换为 Callable 再包装
                Supplier<?> original = supplier;
                supplier = () -> {
                    Callable<?> callable = () -> original.get();
                    Callable<?> wrapped = TtlCallable.get(callable);
                    try {
                        return wrapped.call();
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }
                };
            }
        }
    }

    // ── runAsync ──────────────────────────────────────────

    public static class RunAsyncAdvice {
        @Advice.OnMethodEnter
        public static void onEnter(@Advice.Argument(value = 0, readOnly = false) Runnable runnable) {
            if (runnable != null && !(runnable instanceof TtlRunnable)) {
                String tag = GrayContext.get();
                GrayTraceLogger.logAsync(tag, "CompletableFuture", "Runnable", Thread.currentThread().getName());
                runnable = TtlRunnable.get(runnable);
            }
        }
    }

    // ── thenApplyAsync ────────────────────────────────────

    public static class ThenApplyAsyncAdvice {
        @Advice.OnMethodEnter
        public static void onEnter(@Advice.Argument(value = 0, readOnly = false) Function<?, ?> fn) {
            if (fn != null) {
                String tag = GrayContext.get();
                GrayTraceLogger.logAsync(tag, "CompletableFuture", "Function", Thread.currentThread().getName());
                // 捕获当前灰度上下文，在执行时恢复
                String capturedTag = GrayContext.get();
                Function<Object, Object> original = (Function<Object, Object>) fn;
                fn = input -> {
                    GrayContext.set(capturedTag);
                    try {
                        return original.apply(input);
                    } finally {
                        GrayContext.clear();
                    }
                };
            }
        }
    }

    // ── thenAcceptAsync ───────────────────────────────────

    public static class ThenAcceptAsyncAdvice {
        @Advice.OnMethodEnter
        public static void onEnter(@Advice.Argument(value = 0, readOnly = false) Consumer<?> consumer) {
            if (consumer != null) {
                String tag = GrayContext.get();
                GrayTraceLogger.logAsync(tag, "CompletableFuture", "Consumer", Thread.currentThread().getName());
                // 捕获当前灰度上下文，在执行时恢复
                String capturedTag = GrayContext.get();
                Consumer<Object> original = (Consumer<Object>) consumer;
                consumer = input -> {
                    GrayContext.set(capturedTag);
                    try {
                        original.accept(input);
                    } finally {
                        GrayContext.clear();
                    }
                };
            }
        }
    }

    // ── thenRunAsync ──────────────────────────────────────

    public static class ThenRunAsyncAdvice {
        @Advice.OnMethodEnter
        public static void onEnter(@Advice.Argument(value = 0, readOnly = false) Runnable runnable) {
            if (runnable != null && !(runnable instanceof TtlRunnable)) {
                String tag = GrayContext.get();
                GrayTraceLogger.logAsync(tag, "CompletableFuture", "Runnable", Thread.currentThread().getName());
                runnable = TtlRunnable.get(runnable);
            }
        }
    }
}