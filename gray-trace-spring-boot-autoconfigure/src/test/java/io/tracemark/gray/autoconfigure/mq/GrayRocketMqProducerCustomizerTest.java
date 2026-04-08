package io.tracemark.gray.autoconfigure.mq;

import org.apache.rocketmq.spring.core.RocketMQTemplate;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * GrayRocketMqProducerCustomizer 单元测试
 */
class GrayRocketMqProducerCustomizerTest {

    private GrayRocketMqProducerCustomizer customizer;

    @Test
    @DisplayName("非 RocketMQTemplate 类型应直接返回原 Bean")
    void postProcessAfterInitialization_nonTemplate_shouldReturnOriginal() {
        customizer = new GrayRocketMqProducerCustomizer();

        Object originalBean = new Object();
        Object result = customizer.postProcessAfterInitialization(originalBean, "testBean");

        assertSame(originalBean, result);
    }

    @Test
    @DisplayName("RocketMQTemplate 类型应尝试注册 Hook")
    void postProcessAfterInitialization_template_shouldAttemptRegister() {
        customizer = new GrayRocketMqProducerCustomizer();

        // Mock RocketMQTemplate
        RocketMQTemplate mockTemplate = mock(RocketMQTemplate.class);
        // Mock producer 返回 null（无法注册 hook，但不抛异常）
        when(mockTemplate.getProducer()).thenReturn(null);

        Object result = customizer.postProcessAfterInitialization(mockTemplate, "testTemplate");

        assertSame(mockTemplate, result);
    }
}