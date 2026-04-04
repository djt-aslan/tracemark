package io.tracemark.agent.transformer;

import io.tracemark.agent.advice.CompletableFutureAsyncAdvice;
import net.bytebuddy.agent.builder.AgentBuilder;
import net.bytebuddy.asm.Advice;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.dynamic.DynamicType;
import net.bytebuddy.matcher.ElementMatchers;
import net.bytebuddy.utility.JavaModule;

import java.security.ProtectionDomain;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * Transformer：将 {@link CompletableFutureAsyncAdvice} 绑定到
 * {@code java.util.concurrent.CompletableFuture} 的各异步方法。
 */
public class CompletableFutureAsyncTransformer implements AgentBuilder.Transformer {

    @Override
    public DynamicType.Builder<?> transform(DynamicType.Builder<?> builder,
                                             TypeDescription typeDescription,
                                             ClassLoader classLoader,
                                             JavaModule module,
                                             ProtectionDomain protectionDomain) {
        return builder
                // supplyAsync(Supplier) / supplyAsync(Supplier, Executor)
                .visit(Advice.to(CompletableFutureAsyncAdvice.SupplyAsyncAdvice.class)
                        .on(ElementMatchers.named("supplyAsync")
                                .and(ElementMatchers.takesArgument(0, Supplier.class))))
                // runAsync(Runnable) / runAsync(Runnable, Executor)
                .visit(Advice.to(CompletableFutureAsyncAdvice.RunAsyncAdvice.class)
                        .on(ElementMatchers.named("runAsync")
                                .and(ElementMatchers.takesArgument(0, Runnable.class))))
                // thenApplyAsync(Function) / thenApplyAsync(Function, Executor)
                .visit(Advice.to(CompletableFutureAsyncAdvice.ThenApplyAsyncAdvice.class)
                        .on(ElementMatchers.named("thenApplyAsync")
                                .and(ElementMatchers.takesArgument(0, Function.class))))
                // thenAcceptAsync(Consumer) / thenAcceptAsync(Consumer, Executor)
                .visit(Advice.to(CompletableFutureAsyncAdvice.ThenAcceptAsyncAdvice.class)
                        .on(ElementMatchers.named("thenAcceptAsync")
                                .and(ElementMatchers.takesArgument(0, Consumer.class))))
                // thenRunAsync(Runnable) / thenRunAsync(Runnable, Executor)
                .visit(Advice.to(CompletableFutureAsyncAdvice.ThenRunAsyncAdvice.class)
                        .on(ElementMatchers.named("thenRunAsync")
                                .and(ElementMatchers.takesArgument(0, Runnable.class))));
    }
}
