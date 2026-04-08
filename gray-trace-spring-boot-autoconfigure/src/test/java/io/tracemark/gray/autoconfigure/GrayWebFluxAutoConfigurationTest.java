package io.tracemark.gray.autoconfigure;

import io.tracemark.gray.autoconfigure.http.GrayWebClientCustomizer;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * GrayWebFluxAutoConfiguration 单元测试
 */
class GrayWebFluxAutoConfigurationTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(GrayWebFluxAutoConfiguration.class));

    @Test
    @DisplayName("默认配置应加载 GrayWebClientCustomizer")
    void defaultConfig_shouldLoadWebClientCustomizer() {
        contextRunner.run(context -> {
            assertThat(context).hasSingleBean(GrayWebClientCustomizer.class);
        });
    }

    @Test
    @DisplayName("enabled=false 时不加载")
    void disabled_shouldNotLoad() {
        contextRunner
                .withPropertyValues("gray.trace.enabled=false")
                .run(context -> {
                    assertThat(context).doesNotHaveBean(GrayWebClientCustomizer.class);
                });
    }

    @Test
    @DisplayName("web-flux.enabled=false 时不加载")
    void webFluxDisabled_shouldNotLoad() {
        contextRunner
                .withPropertyValues("gray.trace.web-flux.enabled=false")
                .run(context -> {
                    assertThat(context).doesNotHaveBean(GrayWebClientCustomizer.class);
                });
    }
}