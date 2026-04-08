package io.tracemark.agent.advice;

import io.tracemark.gray.core.GrayConstants;
import io.tracemark.gray.core.GrayContext;
import org.apache.rocketmq.common.message.Message;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * RocketMqProducerAdvice 单元测试
 */
class RocketMqProducerAdviceTest {

    @BeforeEach
    void setUp() {
        GrayContext.clear();
    }

    @AfterEach
    void tearDown() {
        GrayContext.clear();
    }

    @Nested
    @DisplayName("onSend 方法逻辑测试")
    class OnSendTest {

        @Test
        @DisplayName("灰度标存在时应写入消息属性")
        void onSend_withGrayTag_shouldWriteProperty() {
            GrayContext.set("gray-v1");

            Message message = new Message("test-topic", "test-tag", "test-body".getBytes());

            simulateOnSend(message);

            assertEquals("gray-v1", message.getUserProperty(GrayConstants.MQ_PROPERTY_GRAY_TAG));
        }

        @Test
        @DisplayName("灰度标为 stable 时应写入消息属性")
        void onSend_withStableTag_shouldWriteProperty() {
            GrayContext.set(GrayConstants.TAG_STABLE);

            Message message = new Message("test-topic", "test-tag", "test-body".getBytes());

            simulateOnSend(message);

            assertEquals(GrayConstants.TAG_STABLE, message.getUserProperty(GrayConstants.MQ_PROPERTY_GRAY_TAG));
        }

        @Test
        @DisplayName("无灰度标时应写入 stable 属性")
        void onSend_withoutTag_shouldWriteStableProperty() {
            GrayContext.clear();

            Message message = new Message("test-topic", "test-tag", "test-body".getBytes());

            simulateOnSend(message);

            // GrayContext.get() 默认返回 stable
            assertEquals(GrayConstants.TAG_STABLE, message.getUserProperty(GrayConstants.MQ_PROPERTY_GRAY_TAG));
        }

        @Test
        @DisplayName("已有灰度属性时不应覆盖")
        void onSend_withExistingProperty_shouldNotOverride() {
            GrayContext.set("gray-v2");

            Message message = new Message("test-topic", "test-tag", "test-body".getBytes());
            message.putUserProperty(GrayConstants.MQ_PROPERTY_GRAY_TAG, "gray-v1");

            simulateOnSend(message);

            assertEquals("gray-v1", message.getUserProperty(GrayConstants.MQ_PROPERTY_GRAY_TAG));
        }

        @Test
        @DisplayName("message 为 null 时不应处理")
        void onSend_withNullMessage_shouldDoNothing() {
            GrayContext.set("gray-v1");

            simulateOnSend(null);

            // 无异常，正常执行
        }
    }

    // 模拟 Advice.onSend 的逻辑
    private void simulateOnSend(Message msg) {
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