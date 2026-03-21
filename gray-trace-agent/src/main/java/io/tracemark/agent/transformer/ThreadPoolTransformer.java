package io.tracemark.agent.transformer;

import io.tracemark.agent.advice.ThreadPoolAdvice;
import net.bytebuddy.agent.builder.AgentBuilder;
import net.bytebuddy.asm.Advice;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.dynamic.DynamicType;
import net.bytebuddy.matcher.ElementMatchers;
import net.bytebuddy.utility.JavaModule;

import java.security.ProtectionDomain;

/**
 * 插桩 ThreadPoolExecutor#execute / submit，用 TTL 包装任务
 */
public class ThreadPoolTransformer implements AgentBuilder.Transformer {

    @Override
    public DynamicType.Builder<?> transform(DynamicType.Builder<?> builder,
                                             TypeDescription typeDescription,
                                             ClassLoader classLoader,
                                             JavaModule module,
                                             ProtectionDomain protectionDomain) {
        return builder
                // execute(Runnable)
                .visit(Advice.to(ThreadPoolAdvice.class)
                        .on(ElementMatchers.named("execute")
                                .and(ElementMatchers.takesArgument(0, Runnable.class))))
                // submit(Callable)
                .visit(Advice.to(ThreadPoolAdvice.CallableAdvice.class)
                        .on(ElementMatchers.named("submit")
                                .and(ElementMatchers.takesArgument(0,
                                        ElementMatchers.isSubTypeOf(java.util.concurrent.Callable.class)))));
    }
}