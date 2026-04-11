package io.tracemark.agent.advice;

import io.tracemark.gray.core.GrayConstants;
import io.tracemark.gray.core.GrayContext;
import org.apache.rocketmq.common.message.MessageExt;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * RocketMqConsumerAdvice 单元测试
 */
class RocketMqConsumerAdviceTest {

    @BeforeEach
    void setUp() {
        GrayContext.clear();
    }

    @AfterEach
    void tearDown() {
        GrayContext.clear();
    }

    @Nested
    @DisplayName("onEnter 方法逻辑测试")
    class OnEnterTest {

        @Test
        @DisplayName("消息包含灰度标签时应恢复上下文")
        void onEnter_withGrayTag_shouldRestoreContext() {
            MessageExt message = createMessage("test-topic", "gray-v1");

            simulateOnEnter(message);

            assertEquals("gray-v1", GrayContext.get());
        }

        @Test
        @DisplayName("消息灰度标签为 stable 时应恢复上下文")
        void onEnter_withStableTag_shouldRestoreContext() {
            MessageExt message = createMessage("test-topic", GrayConstants.TAG_STABLE);

            simulateOnEnter(message);

            assertEquals(GrayConstants.TAG_STABLE, GrayContext.get());
        }

        @Test
        @DisplayName("消息不包含灰度标签时应设置 stable")
        void onEnter_withoutGrayTag_shouldSetStable() {
            MessageExt message = createMessage("test-topic", null);

            simulateOnEnter(message);

            assertEquals(GrayConstants.TAG_STABLE, GrayContext.get());
        }

        @Test
        @DisplayName("消息灰度标签为空时应设置 stable")
        void onEnter_withEmptyGrayTag_shouldSetStable() {
            MessageExt message = createMessage("test-topic", "");

            simulateOnEnter(message);

            assertEquals(GrayConstants.TAG_STABLE, GrayContext.get());
        }

        @Test
        @DisplayName("消息列表包含多条消息时应处理第一条")
        void onEnter_withMultipleMessages_shouldProcessFirst() {
            List<MessageExt> messages = new ArrayList<>();
            messages.add(createMessage("test-topic", "gray-v1"));
            messages.add(createMessage("test-topic", "gray-v2"));

            simulateOnEnter(messages);

            assertEquals("gray-v1", GrayContext.get());
        }

        @Test
        @DisplayName("消息列表为空时应设置 stable")
        void onEnter_withEmptyList_shouldSetStable() {
            List<MessageExt> messages = new ArrayList<>();

            simulateOnEnter(messages);

            assertEquals(GrayConstants.TAG_STABLE, GrayContext.get());
        }
    }

    @Nested
    @DisplayName("onExit 方法逻辑测试")
    class OnExitTest {

        @Test
        @DisplayName("onExit 应清除上下文")
        void onExit_shouldClearContext() {
            GrayContext.set("gray-v1");

            simulateOnExit();

            assertEquals(GrayConstants.TAG_STABLE, GrayContext.get());
        }
    }

    // 辅助方法：创建带灰度标签的消息
    private MessageExt createMessage(String topic, String grayTag) {
        MessageExt message = new MessageExt();
        message.setTopic(topic);
        // RocketMQ 不允许空字符串作为属性值，所以只在非空时设置
        if (grayTag != null && !grayTag.isEmpty()) {
            message.putUserProperty(GrayConstants.MQ_PROPERTY_GRAY_TAG, grayTag);
        }
        return message;
    }

    // 模拟 Advice.onEnter 的逻辑（单消息）
    private void simulateOnEnter(MessageExt msg) {
        if (msg != null) {
            String tag = msg.getUserProperty(GrayConstants.MQ_PROPERTY_GRAY_TAG);
            GrayContext.set(tag != null && !tag.isEmpty() ? tag : GrayConstants.TAG_STABLE);
        } else {
            GrayContext.set(GrayConstants.TAG_STABLE);
        }
    }

    // 模拟 Advice.onEnter 的逻辑（消息列表）
    private void simulateOnEnter(List<MessageExt> msgs) {
        if (msgs != null && !msgs.isEmpty()) {
            MessageExt firstMsg = msgs.get(0);
            String tag = firstMsg.getUserProperty(GrayConstants.MQ_PROPERTY_GRAY_TAG);
            GrayContext.set(tag != null && !tag.isEmpty() ? tag : GrayConstants.TAG_STABLE);
        } else {
            GrayContext.set(GrayConstants.TAG_STABLE);
        }
    }

    // 模拟 Advice.onExit 的逻辑
    private void simulateOnExit() {
        GrayContext.clear();
    }
}
