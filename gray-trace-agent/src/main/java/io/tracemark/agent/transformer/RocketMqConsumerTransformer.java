package io.tracemark.agent.transformer;

import io.tracemark.agent.advice.RocketMqConsumerAdvice;
import net.bytebuddy.agent.builder.AgentBuilder;
import net.bytebuddy.asm.Advice;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.dynamic.DynamicType;
import net.bytebuddy.matcher.ElementMatchers;
import net.bytebuddy.utility.JavaModule;

import java.security.ProtectionDomain;

/**
 * Transformer：拦截 RocketMQ PullAPIWrapper 恢复灰度上下文
 *
 * <p>目标类：{@code org.apache.rocketmq.client.impl.consumer.PullAPIWrapper}
 * <p>目标方法：{@code processPullResult}
 *
 * <p>该方法是消息拉取后处理的统一入口，覆盖 Push/Pull 两种消费模式。
 */
public class RocketMqConsumerTransformer implements AgentBuilder.Transformer {

    @Override
    public DynamicType.Builder<?> transform(DynamicType.Builder<?> builder,
                                             TypeDescription typeDescription,
                                             ClassLoader classLoader,
                                             JavaModule module,
                                             ProtectionDomain protectionDomain) {
        return builder.visit(
                Advice.to(RocketMqConsumerAdvice.class)
                        .on(ElementMatchers.named("processPullResult")));
    }
}
