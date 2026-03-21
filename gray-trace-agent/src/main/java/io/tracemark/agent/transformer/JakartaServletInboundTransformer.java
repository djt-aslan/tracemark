package io.tracemark.agent.transformer;

import io.tracemark.agent.advice.JakartaServletInboundAdvice;
import net.bytebuddy.agent.builder.AgentBuilder;
import net.bytebuddy.asm.Advice;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.dynamic.DynamicType;
import net.bytebuddy.matcher.ElementMatchers;
import net.bytebuddy.utility.JavaModule;

import java.security.ProtectionDomain;

/** jakarta.servlet.http.HttpServlet 插桩 Transformer（Spring Boot 3.x）*/
public class JakartaServletInboundTransformer implements AgentBuilder.Transformer {

    @Override
    public DynamicType.Builder<?> transform(DynamicType.Builder<?> builder,
                                             TypeDescription typeDescription,
                                             ClassLoader classLoader,
                                             JavaModule module,
                                             ProtectionDomain protectionDomain) {
        return builder.visit(
                Advice.to(JakartaServletInboundAdvice.class)
                        .on(ElementMatchers.named("service")
                                .and(ElementMatchers.takesArguments(2))));
    }
}