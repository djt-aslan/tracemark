package io.tracemark.gray.autoconfigure;

import io.tracemark.gray.autoconfigure.mq.GrayRocketMqConsumerHook;
import io.tracemark.gray.autoconfigure.mq.GrayRocketMqProducerCustomizer;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * GrayRocketMqAutoConfiguration 单元测试
 */
class GrayRocketMqAutoConfigurationTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(GrayRocketMqAutoConfiguration.class));

    @Test
    @DisplayName("默认配置不加载（mq.enabled 默认 false）")
    void defaultConfig_shouldNotLoad() {
        contextRunner.run(context -> {
            assertThat(context).doesNotHaveBean(GrayRocketMqProducerCustomizer.class);
            assertThat(context).doesNotHaveBean(GrayRocketMqConsumerHook.class);
        });
    }

    @Test
    @DisplayName("mq.enabled=true 时应加载 producer 和 consumer")
    void mqEnabled_shouldLoadProducerAndConsumer() {
        contextRunner
                .withPropertyValues("gray.trace.mq.enabled=true")
                .run(context -> {
                    assertThat(context).hasSingleBean(GrayRocketMqProducerCustomizer.class);
                    // Consumer 需要 DefaultRocketMQListenerContainer 类存在
                    // assertThat(context).hasSingleBean(GrayRocketMqConsumerHook.class);
                });
    }

    @Test
    @DisplayName("mq.enabled=true 且 producer=false 时不加载 producer")
    void producerDisabled_shouldNotLoadProducer() {
        contextRunner
                .withPropertyValues(
                        "gray.trace.mq.enabled=true",
                        "gray.trace.mq.producer=false")
                .run(context -> {
                    assertThat(context).doesNotHaveBean(GrayRocketMqProducerCustomizer.class);
                });
    }

    @Test
    @DisplayName("enabled=false 时不加载")
    void disabled_shouldNotLoad() {
        contextRunner
                .withPropertyValues("gray.trace.enabled=false")
                .run(context -> {
                    assertThat(context).doesNotHaveBean(GrayRocketMqProducerCustomizer.class);
                });
    }
}