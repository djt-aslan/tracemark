package io.tracemark.agent.advice;

import com.alibaba.ttl.TtlCallable;
import com.alibaba.ttl.TtlRunnable;
import net.bytebuddy.asm.Advice;

import java.util.concurrent.Callable;

/**
 * Advice：拦截 ThreadPoolExecutor#execute 和 #submit，
 * 用 TTL 包装 Runnable/Callable，保证线程池复用时上下文不丢失。
 */
public class ThreadPoolAdvice {

    @Advice.OnMethodEnter
    public static void onExecute(@Advice.Argument(value = 0, readOnly = false) Runnable runnable) {
        if (runnable != null && !(runnable instanceof TtlRunnable)) {
            runnable = TtlRunnable.get(runnable);
        }
    }

    // submit(Callable) 重载
    public static class CallableAdvice {
        @Advice.OnMethodEnter
        public static <T> void onSubmit(
                @Advice.Argument(value = 0, readOnly = false) Callable<T> callable) {
            if (callable != null && !(callable instanceof TtlCallable)) {
                callable = TtlCallable.get(callable);
            }
        }
    }
}