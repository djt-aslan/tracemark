package io.tracemark.agent.transformer;

import io.tracemark.agent.advice.JdkHttpClientOutboundAdvice;
import net.bytebuddy.agent.builder.AgentBuilder;
import net.bytebuddy.asm.Advice;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.dynamic.DynamicType;
import net.bytebuddy.matcher.ElementMatchers;
import net.bytebuddy.utility.JavaModule;

import java.security.ProtectionDomain;

/**
 * Transformer：拦截 java.net.http.HttpClient 的 send/sendAsync 方法
 *
 * <p>目标类：{@code java.net.http.HttpClient}
 * <p>目标方法：{@code send}, {@code sendAsync}
 */
public class JdkHttpClientOutboundTransformer implements AgentBuilder.Transformer {

    @Override
    public DynamicType.Builder<?> transform(DynamicType.Builder<?> builder,
                                             TypeDescription typeDescription,
                                             ClassLoader classLoader,
                                             JavaModule module,
                                             ProtectionDomain protectionDomain) {
        return builder.visit(
                Advice.to(JdkHttpClientOutboundAdvice.class)
                        .on(ElementMatchers.named("send")
                                .or(ElementMatchers.named("sendAsync"))));
    }
}
