package io.tracemark.gray.autoconfigure.mq;

import io.tracemark.gray.core.GrayConstants;
import io.tracemark.gray.core.GrayContext;
import org.apache.rocketmq.spring.core.RocketMQTemplate;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanPostProcessor;

/**
 * 自动将 {@link GrayRocketMqSendHook} 注册到 Spring 管理的 {@link RocketMQTemplate}
 *
 * <p>在 RocketMQTemplate 初始化完成后，通过 BeanPostProcessor 注入 Hook，实现无侵入。
 */
public class GrayRocketMqProducerCustomizer implements BeanPostProcessor {

    private final GrayRocketMqSendHook sendHook = new GrayRocketMqSendHook();

    @Override
    public Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException {
        if (bean instanceof RocketMQTemplate) {
            RocketMQTemplate template = (RocketMQTemplate) bean;
            try {
                // getProducer() 返回 DefaultMQProducer
                org.apache.rocketmq.client.producer.DefaultMQProducer producer =
                        template.getProducer();
                if (producer != null) {
                    producer.getDefaultMQProducerImpl().registerSendMessageHook(sendHook);
                }
            } catch (Exception e) {
                // 忽略注册失败，不影响业务
            }
        }
        return bean;
    }
}