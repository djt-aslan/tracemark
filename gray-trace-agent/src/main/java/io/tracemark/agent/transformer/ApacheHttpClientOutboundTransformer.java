package io.tracemark.agent.transformer;

import io.tracemark.agent.advice.ApacheHttpClientOutboundAdvice;
import net.bytebuddy.agent.builder.AgentBuilder;
import net.bytebuddy.asm.Advice;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.dynamic.DynamicType;
import net.bytebuddy.matcher.ElementMatchers;
import net.bytebuddy.utility.JavaModule;

import java.security.ProtectionDomain;

/**
 * Transformer：将 {@link ApacheHttpClientOutboundAdvice} 绑定到
 * {@code org.apache.http.impl.client.CloseableHttpClient} 的所有 {@code execute} 重载。
 */
public class ApacheHttpClientOutboundTransformer implements AgentBuilder.Transformer {

    @Override
    public DynamicType.Builder<?> transform(DynamicType.Builder<?> builder,
                                             TypeDescription typeDescription,
                                             ClassLoader classLoader,
                                             JavaModule module,
                                             ProtectionDomain protectionDomain) {
        return builder.visit(
                Advice.to(ApacheHttpClientOutboundAdvice.class)
                        .on(ElementMatchers.named("execute")));
    }
}
