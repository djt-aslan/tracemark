package io.tracemark.agent.advice;

import io.tracemark.agent.GrayTraceLogger;
import io.tracemark.gray.core.GrayConstants;
import io.tracemark.gray.core.GrayContext;
import net.bytebuddy.asm.Advice;
import org.apache.rocketmq.common.message.MessageExt;

import java.util.List;

/**
 * Advice：拦截 RocketMQ 消息消费方法，从消息属性恢复灰度上下文。
 *
 * <p>插桩目标：{@code org.apache.rocketmq.client.impl.consumer.PullAPIWrapper#processPullResult}
 *
 * <p>在消息处理前从 UserProperty 提取 grayTag 设置到 GrayContext，
 * 处理后清理上下文，防止线程池复用时污染。
 */
public class RocketMqConsumerAdvice {

    @Advice.OnMethodEnter
    public static void onEnter(@Advice.Argument(0) List<MessageExt> msgs) {
        if (msgs != null && !msgs.isEmpty()) {
            MessageExt firstMsg = msgs.get(0);
            String tag = firstMsg.getUserProperty(GrayConstants.MQ_PROPERTY_GRAY_TAG);
            String effectiveTag = tag != null && !tag.isEmpty() ? tag : GrayConstants.TAG_STABLE;
            GrayContext.set(effectiveTag);

            // 日志输出
            GrayTraceLogger.logMqConsumer(effectiveTag, firstMsg.getTopic(), Thread.currentThread().getName());
        } else {
            GrayContext.set(GrayConstants.TAG_STABLE);
        }
    }

    @Advice.OnMethodExit(onThrowable = Throwable.class)
    public static void onExit() {
        String tag = GrayContext.get();
        GrayTraceLogger.logClear(tag, Thread.currentThread().getName());
        GrayContext.clear();
    }
}
