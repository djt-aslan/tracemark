package io.tracemark.agent.advice;

import io.tracemark.gray.core.GrayConstants;
import io.tracemark.gray.core.GrayContext;
import net.bytebuddy.asm.Advice;
import org.apache.rocketmq.common.message.Message;

/**
 * Advice：拦截 RocketMQ DefaultMQProducerImpl#sendKernelImpl，
 * 在消息发送前将灰度标签写入 UserProperty。
 */
public class RocketMqProducerAdvice {

    @Advice.OnMethodEnter
    public static void onSend(@Advice.Argument(0) Message msg) {
        if (msg == null) {
            return;
        }
        String tag = GrayContext.get();
        if (tag != null && !tag.isEmpty()
                && msg.getUserProperty(GrayConstants.MQ_PROPERTY_GRAY_TAG) == null) {
            msg.putUserProperty(GrayConstants.MQ_PROPERTY_GRAY_TAG, tag);
        }
    }
}