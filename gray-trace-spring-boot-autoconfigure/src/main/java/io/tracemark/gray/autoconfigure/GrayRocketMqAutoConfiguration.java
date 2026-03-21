package io.tracemark.gray.autoconfigure;

import io.tracemark.gray.autoconfigure.mq.GrayRocketMqConsumerHook;
import io.tracemark.gray.autoconfigure.mq.GrayRocketMqProducerCustomizer;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * RocketMQ 灰度配置（仅在 rocketmq-spring-boot-starter 存在时加载，默认关闭）
 */
@Configuration(proxyBeanMethods = false)
@ConditionalOnClass(name = "org.apache.rocketmq.spring.core.RocketMQTemplate")
@ConditionalOnProperty(prefix = "gray.trace", name = "enabled", matchIfMissing = true)
public class GrayRocketMqAutoConfiguration {

    @Bean
    @ConditionalOnExpression("${gray.trace.mq.enabled:false} and ${gray.trace.mq.producer:true}")
    public GrayRocketMqProducerCustomizer grayRocketMqProducerCustomizer() {
        return new GrayRocketMqProducerCustomizer();
    }

    @Bean
    @ConditionalOnClass(name = "org.apache.rocketmq.spring.support.DefaultRocketMQListenerContainer")
    @ConditionalOnExpression("${gray.trace.mq.enabled:false} and ${gray.trace.mq.consumer:true}")
    public GrayRocketMqConsumerHook grayRocketMqConsumerHook() {
        return new GrayRocketMqConsumerHook();
    }
}