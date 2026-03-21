package io.tracemark.gray.autoconfigure.mq;

import io.tracemark.gray.core.GrayConstants;
import io.tracemark.gray.core.GrayContext;
import org.apache.rocketmq.spring.core.RocketMQTemplate;
import org.apache.rocketmq.spring.support.DefaultRocketMQListenerContainer;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanPostProcessor;

import java.lang.reflect.Field;

/**
 * RocketMQ 消费者灰度上下文恢复 Hook
 *
 * <p>通过包装 {@link DefaultRocketMQListenerContainer} 中的消息监听器，
 * 在消费消息前从消息 UserProperty 中读取灰度标签并恢复上下文，
 * 消费完成后清理，保证线程安全。
 *
 * <p>本类通过 AOP 方式植入，无需修改消费者代码。
 */
public class GrayRocketMqConsumerHook implements BeanPostProcessor {

    @Override
    public Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException {
        if (!(bean instanceof DefaultRocketMQListenerContainer)) {
            return bean;
        }

        DefaultRocketMQListenerContainer container = (DefaultRocketMQListenerContainer) bean;

        // 使用装饰模式包装原始 MessageListener
        wrapMessageListener(container);

        return bean;
    }

    private void wrapMessageListener(DefaultRocketMQListenerContainer container) {
        try {
            // 获取内部 rocketMQListener 字段，包装之
            Field listenerField = DefaultRocketMQListenerContainer.class
                    .getDeclaredField("rocketMQListener");
            listenerField.setAccessible(true);
            Object originalListener = listenerField.get(container);

            if (originalListener == null || isAlreadyWrapped(originalListener)) {
                return;
            }

            // 使用动态代理包装原始监听器
            Object wrappedListener = wrapWithGrayContext(originalListener);
            listenerField.set(container, wrappedListener);
        } catch (Exception e) {
            // 包装失败不影响消费，仅灰度上下文不传递
        }
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private Object wrapWithGrayContext(Object original) {
        // 通过 Java 动态代理包装 RocketMQListener
        return java.lang.reflect.Proxy.newProxyInstance(
                original.getClass().getClassLoader(),
                original.getClass().getInterfaces(),
                (proxy, method, args) -> {
                    if ("onMessage".equals(method.getName()) && args != null && args.length == 1) {
                        Object msg = args[0];
                        String grayTag = extractGrayTag(msg);
                        try {
                            GrayContext.set(grayTag);
                            return method.invoke(original, args);
                        } finally {
                            GrayContext.clear();
                        }
                    }
                    return method.invoke(original, args);
                });
    }

    private String extractGrayTag(Object message) {
        try {
            // 支持 MessageExt 和 org.springframework.messaging.Message 两种类型
            if (message instanceof org.apache.rocketmq.common.message.MessageExt) {
                String tag = ((org.apache.rocketmq.common.message.MessageExt) message)
                        .getUserProperty(GrayConstants.MQ_PROPERTY_GRAY_TAG);
                return tag != null ? tag : GrayConstants.TAG_STABLE;
            }
            // Spring Message wrapper
            if (message instanceof org.springframework.messaging.Message) {
                Object headerValue = ((org.springframework.messaging.Message<?>) message)
                        .getHeaders().get(GrayConstants.MQ_PROPERTY_GRAY_TAG);
                return headerValue != null ? headerValue.toString() : GrayConstants.TAG_STABLE;
            }
        } catch (Exception ignored) {}
        return GrayConstants.TAG_STABLE;
    }

    private boolean isAlreadyWrapped(Object listener) {
        return java.lang.reflect.Proxy.isProxyClass(listener.getClass());
    }
}