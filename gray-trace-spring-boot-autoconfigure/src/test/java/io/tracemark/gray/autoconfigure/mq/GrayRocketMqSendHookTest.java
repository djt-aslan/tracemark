package io.tracemark.gray.autoconfigure.mq;

import io.tracemark.gray.core.GrayConstants;
import io.tracemark.gray.core.GrayContext;
import org.apache.rocketmq.client.hook.SendMessageContext;
import org.apache.rocketmq.common.message.Message;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * GrayRocketMqSendHook 单元测试
 */
class GrayRocketMqSendHookTest {

    private GrayRocketMqSendHook sendHook;

    @BeforeEach
    void setUp() {
        sendHook = new GrayRocketMqSendHook();
        GrayContext.clear();
    }

    @AfterEach
    void tearDown() {
        GrayContext.clear();
    }

    @Nested
    @DisplayName("sendMessageBefore 方法测试")
    class SendMessageBeforeTest {

        @Test
        @DisplayName("hookName 应返回正确名称")
        void hookName_shouldReturnCorrectName() {
            assertEquals("GrayTraceSendHook", sendHook.hookName());
        }

        @Test
        @DisplayName("灰度标存在时应写入消息属性")
        void sendMessageBefore_withGrayTag_shouldWriteProperty() {
            GrayContext.set("gray-v1");

            Message message = new Message("test-topic", "test-tag", "test-body".getBytes());
            SendMessageContext context = new SendMessageContext();
            context.setMessage(message);

            sendHook.sendMessageBefore(context);

            assertEquals("gray-v1", message.getUserProperty(GrayConstants.MQ_PROPERTY_GRAY_TAG));
        }

        @Test
        @DisplayName("灰度标为 stable 时应写入消息属性")
        void sendMessageBefore_withStableTag_shouldWriteProperty() {
            GrayContext.set(GrayConstants.TAG_STABLE);

            Message message = new Message("test-topic", "test-tag", "test-body".getBytes());
            SendMessageContext context = new SendMessageContext();
            context.setMessage(message);

            sendHook.sendMessageBefore(context);

            assertEquals(GrayConstants.TAG_STABLE, message.getUserProperty(GrayConstants.MQ_PROPERTY_GRAY_TAG));
        }

        @Test
        @DisplayName("无灰度标时应写入 stable 属性")
        void sendMessageBefore_withoutTag_shouldWriteStableProperty() {
            GrayContext.clear();

            Message message = new Message("test-topic", "test-tag", "test-body".getBytes());
            SendMessageContext context = new SendMessageContext();
            context.setMessage(message);

            sendHook.sendMessageBefore(context);

            // GrayContext.get() 默认返回 stable
            assertEquals(GrayConstants.TAG_STABLE, message.getUserProperty(GrayConstants.MQ_PROPERTY_GRAY_TAG));
        }

        @Test
        @DisplayName("已有灰度属性时会被覆盖")
        void sendMessageBefore_withExistingProperty_shouldOverride() {
            GrayContext.set("gray-v2");

            Message message = new Message("test-topic", "test-tag", "test-body".getBytes());
            message.putUserProperty(GrayConstants.MQ_PROPERTY_GRAY_TAG, "gray-v1");
            SendMessageContext context = new SendMessageContext();
            context.setMessage(message);

            sendHook.sendMessageBefore(context);

            // 当前实现会覆盖已有属性
            assertEquals("gray-v2", message.getUserProperty(GrayConstants.MQ_PROPERTY_GRAY_TAG));
        }

        @Test
        @DisplayName("context 为 null 时不应处理")
        void sendMessageBefore_withNullContext_shouldDoNothing() {
            GrayContext.set("gray-v1");

            sendHook.sendMessageBefore(null);

            // 无异常，正常执行
        }

        @Test
        @DisplayName("message 为 null 时不应处理")
        void sendMessageBefore_withNullMessage_shouldDoNothing() {
            GrayContext.set("gray-v1");

            SendMessageContext context = new SendMessageContext();
            context.setMessage(null);

            sendHook.sendMessageBefore(context);

            // 无异常，正常执行
        }
    }

    @Nested
    @DisplayName("sendMessageAfter 方法测试")
    class SendMessageAfterTest {

        @Test
        @DisplayName("sendMessageAfter 应无操作")
        void sendMessageAfter_shouldDoNothing() {
            GrayContext.set("gray-v1");

            Message message = new Message("test-topic", "test-tag", "test-body".getBytes());
            SendMessageContext context = new SendMessageContext();
            context.setMessage(message);

            sendHook.sendMessageAfter(context);

            // 无操作，灰度上下文不变
            assertEquals("gray-v1", GrayContext.get());
        }
    }
}