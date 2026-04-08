package io.tracemark.gray.autoconfigure;

import io.tracemark.gray.core.GrayProperties;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * GrayAutoConfiguration 单元测试
 */
class GrayAutoConfigurationTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(GrayAutoConfiguration.class));

    @Test
    @DisplayName("默认配置应加载 GrayProperties")
    void defaultConfig_shouldLoadGrayProperties() {
        contextRunner.run(context -> {
            assertThat(context).hasSingleBean(GrayProperties.class);
        });
    }

    @Test
    @DisplayName("enabled=false 时不加载")
    void disabled_shouldNotLoad() {
        contextRunner
                .withPropertyValues("gray.trace.enabled=false")
                .run(context -> {
                    assertThat(context).doesNotHaveBean(GrayProperties.class);
                });
    }

    @Test
    @DisplayName("servlet.enabled=false 时不加载 servlet filter")
    void servletDisabled_shouldNotLoadFilter() {
        contextRunner
                .withPropertyValues("gray.trace.servlet.enabled=false")
                .run(context -> {
                    assertThat(context).doesNotHaveBean("grayServletFilter");
                });
    }

    @Test
    @DisplayName("rest-template.enabled=false 时不加载 interceptor")
    void restTemplateDisabled_shouldNotLoadInterceptor() {
        contextRunner
                .withPropertyValues("gray.trace.rest-template.enabled=false")
                .run(context -> {
                    assertThat(context).doesNotHaveBean("grayRestTemplateBeanPostProcessor");
                });
    }

    @Test
    @DisplayName("thread-pool.enabled=false 时不加载 decorator")
    void threadPoolDisabled_shouldNotLoadDecorator() {
        contextRunner
                .withPropertyValues("gray.trace.thread-pool.enabled=false")
                .run(context -> {
                    assertThat(context).doesNotHaveBean("grayExecutorBeanPostProcessor");
                });
    }
}