package io.tracemark.gray.autoconfigure.mq;

import io.tracemark.gray.core.GrayConstants;
import io.tracemark.gray.core.GrayContext;
import org.apache.rocketmq.client.hook.SendMessageContext;
import org.apache.rocketmq.client.hook.SendMessageHook;
import org.apache.rocketmq.common.message.Message;

/**
 * RocketMQ 生产者灰度 Hook
 *
 * <p>在消息发送前将当前灰度标签写入消息 UserProperty，
 * 消费端通过 {@link GrayRocketMqConsumerHook} 恢复上下文。
 *
 * <p>通过 {@link GrayRocketMqProducerCustomizer} 自动注册到 RocketMQTemplate。
 */
public class GrayRocketMqSendHook implements SendMessageHook {

    @Override
    public String hookName() {
        return "GrayTraceSendHook";
    }

    @Override
    public void sendMessageBefore(SendMessageContext context) {
        if (context == null || context.getMessage() == null) {
            return;
        }
        String tag = GrayContext.get();
        if (tag != null && !tag.isEmpty()) {
            Message message = context.getMessage();
            // 写入消息用户属性，消费端可通过 msg.getUserProperty() 读取
            message.putUserProperty(GrayConstants.MQ_PROPERTY_GRAY_TAG, tag);
        }
    }

    @Override
    public void sendMessageAfter(SendMessageContext context) {
        // 无需处理
    }
}