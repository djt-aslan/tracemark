package io.tracemark.agent.transformer;

import io.tracemark.agent.advice.ServletInboundAdvice;
import net.bytebuddy.agent.builder.AgentBuilder;
import net.bytebuddy.asm.Advice;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.dynamic.DynamicType;
import net.bytebuddy.matcher.ElementMatchers;
import net.bytebuddy.utility.JavaModule;

import java.security.ProtectionDomain;

/** javax.servlet.http.HttpServlet 插桩 Transformer */
public class ServletInboundTransformer implements AgentBuilder.Transformer {

    @Override
    public DynamicType.Builder<?> transform(DynamicType.Builder<?> builder,
                                             TypeDescription typeDescription,
                                             ClassLoader classLoader,
                                             JavaModule module,
                                             ProtectionDomain protectionDomain) {
        return builder.visit(
                Advice.to(ServletInboundAdvice.class)
                        .on(ElementMatchers.named("service")
                                .and(ElementMatchers.takesArguments(2))));
    }
}