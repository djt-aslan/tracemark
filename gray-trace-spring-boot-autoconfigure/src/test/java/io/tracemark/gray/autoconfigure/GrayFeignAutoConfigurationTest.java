package io.tracemark.gray.autoconfigure;

import io.tracemark.gray.autoconfigure.http.GrayFeignInterceptor;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * GrayFeignAutoConfiguration 单元测试
 */
class GrayFeignAutoConfigurationTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(GrayFeignAutoConfiguration.class));

    @Test
    @DisplayName("默认配置应加载 GrayFeignInterceptor")
    void defaultConfig_shouldLoadFeignInterceptor() {
        contextRunner.run(context -> {
            assertThat(context).hasSingleBean(GrayFeignInterceptor.class);
        });
    }

    @Test
    @DisplayName("enabled=false 时不加载")
    void disabled_shouldNotLoad() {
        contextRunner
                .withPropertyValues("gray.trace.enabled=false")
                .run(context -> {
                    assertThat(context).doesNotHaveBean(GrayFeignInterceptor.class);
                });
    }

    @Test
    @DisplayName("feign.enabled=false 时不加载")
    void feignDisabled_shouldNotLoad() {
        contextRunner
                .withPropertyValues("gray.trace.feign.enabled=false")
                .run(context -> {
                    assertThat(context).doesNotHaveBean(GrayFeignInterceptor.class);
                });
    }
}