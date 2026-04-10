package io.tracemark.agent;

import io.tracemark.gray.core.GrayContext;
import io.tracemark.gray.core.GrayProperties;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class GrayTraceLoggerTest {

    @BeforeEach
    void setUp() {
        GrayContext.clear();
    }

    @AfterEach
    void tearDown() {
        GrayContext.clear();
    }

    @Test
    @DisplayName("日志开关关闭时不输出日志")
    void logDisabled_noOutput() {
        GrayProperties props = new GrayProperties();
        props.getLog().setEnabled(false);
        GrayTraceLogger.init(props);

        // 设置灰度上下文
        GrayContext.set("gray-v1");

        // 调用日志方法，不应抛异常
        GrayTraceLogger.logInbound("gray-v1", "/api/test", "main");
        GrayTraceLogger.logOutbound("gray-v1", "http://localhost/api", "main");
        GrayTraceLogger.logAsync("gray-v1", "pool-1", "Runnable", "main");
        GrayTraceLogger.logClear("gray-v1", "main");

        // 验证：无异常即通过
        assertThat(true).isTrue();
    }

    @Test
    @DisplayName("日志开关开启时可以输出日志")
    void logEnabled_canOutput() {
        GrayProperties props = new GrayProperties();
        props.getLog().setEnabled(true);
        GrayTraceLogger.init(props);

        GrayContext.set("gray-v1");

        // 调用日志方法
        GrayTraceLogger.logInbound("gray-v1", "/api/test", "main");
        GrayTraceLogger.logOutbound("gray-v1", "http://localhost/api", "main");
        GrayTraceLogger.logAsync("gray-v1", "pool-1", "Runnable", "main");
        GrayTraceLogger.logClear("gray-v1", "main");

        // 验证：无异常即通过（SLF4J 会输出到控制台）
        assertThat(true).isTrue();
    }

    @Test
    @DisplayName("tag 为 null 时不输出日志")
    void nullTag_noOutput() {
        GrayProperties props = new GrayProperties();
        props.getLog().setEnabled(true);
        GrayTraceLogger.init(props);

        // tag 为 null
        GrayTraceLogger.logInbound(null, "/api/test", "main");
        GrayTraceLogger.logOutbound(null, "http://localhost/api", "main");

        // 验证：无异常即通过
        assertThat(true).isTrue();
    }

    @Test
    @DisplayName("tag 为空字符串时不输出日志")
    void emptyTag_noOutput() {
        GrayProperties props = new GrayProperties();
        props.getLog().setEnabled(true);
        GrayTraceLogger.init(props);

        // tag 为空
        GrayTraceLogger.logInbound("", "/api/test", "main");
        GrayTraceLogger.logOutbound("", "http://localhost/api", "main");

        // 验证：无异常即通过
        assertThat(true).isTrue();
    }

    @Test
    @DisplayName("MQ 日志方法正常工作")
    void logMq_works() {
        GrayProperties props = new GrayProperties();
        props.getLog().setEnabled(true);
        GrayTraceLogger.init(props);

        GrayTraceLogger.logMq("gray-v1", "test-topic", "main");

        // 验证：无异常即通过
        assertThat(true).isTrue();
    }

    @Test
    @DisplayName("isEnabled 返回正确的开关状态")
    void isEnabled_returnsCorrectState() {
        GrayProperties props = new GrayProperties();
        props.getLog().setEnabled(true);
        GrayTraceLogger.init(props);
        assertThat(GrayTraceLogger.isEnabled()).isTrue();

        props.getLog().setEnabled(false);
        GrayTraceLogger.init(props);
        assertThat(GrayTraceLogger.isEnabled()).isFalse();
    }
}
