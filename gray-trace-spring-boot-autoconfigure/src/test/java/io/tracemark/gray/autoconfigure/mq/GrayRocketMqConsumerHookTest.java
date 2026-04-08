package io.tracemark.gray.autoconfigure.mq;

import org.apache.rocketmq.spring.support.DefaultRocketMQListenerContainer;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.config.BeanPostProcessor;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * GrayRocketMqConsumerHook 单元测试
 */
class GrayRocketMqConsumerHookTest {

    private GrayRocketMqConsumerHook consumerHook;

    @Test
    @DisplayName("非 DefaultRocketMQListenerContainer 类型应直接返回原 Bean")
    void postProcessAfterInitialization_nonContainer_shouldReturnOriginal() {
        consumerHook = new GrayRocketMqConsumerHook();

        Object originalBean = new Object();
        Object result = consumerHook.postProcessAfterInitialization(originalBean, "testBean");

        assertSame(originalBean, result);
    }

    @Test
    @DisplayName("DefaultRocketMQListenerContainer 类型应尝试包装")
    void postProcessAfterInitialization_container_shouldAttemptWrap() {
        consumerHook = new GrayRocketMqConsumerHook();

        // Mock DefaultRocketMQListenerContainer
        DefaultRocketMQListenerContainer mockContainer = mock(DefaultRocketMQListenerContainer.class);

        // 由于反射获取 rocketMQListener 字段，这里主要验证不抛异常
        Object result = consumerHook.postProcessAfterInitialization(mockContainer, "testContainer");

        // 返回的是原 Bean（因为 mock 的字段为 null）
        assertSame(mockContainer, result);
    }

    @Test
    @DisplayName("BeanPostProcessor 接口方法应正常工作")
    void shouldImplementBeanPostProcessor() {
        consumerHook = new GrayRocketMqConsumerHook();
        assertTrue(consumerHook instanceof BeanPostProcessor);
    }
}