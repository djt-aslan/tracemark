package io.tracemark.agent;

import io.tracemark.gray.core.GrayProperties;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Properties;

import static org.junit.jupiter.api.Assertions.*;

/**
 * AgentConfigLoader 单元测试
 */
class AgentConfigLoaderTest {

    @Test
    @DisplayName("默认配置应加载成功")
    void load_defaultConfig_shouldSucceed() {
        GrayProperties properties = AgentConfigLoader.load();

        assertNotNull(properties);
        assertTrue(properties.isEnabled());
    }

    @Test
    @DisplayName("enabled=false 应正确解析")
    void load_withDisabled_shouldParseCorrectly() {
        System.setProperty("gray.trace.enabled", "false");

        try {
            GrayProperties properties = AgentConfigLoader.load();

            assertNotNull(properties);
            assertFalse(properties.isEnabled());
        } finally {
            System.clearProperty("gray.trace.enabled");
        }
    }

    @Test
    @DisplayName("servlet.enabled=false 应正确解析")
    void load_withServletDisabled_shouldParseCorrectly() {
        System.setProperty("gray.trace.servlet.enabled", "false");

        try {
            GrayProperties properties = AgentConfigLoader.load();

            assertNotNull(properties);
            assertFalse(properties.getServlet().isEnabled());
        } finally {
            System.clearProperty("gray.trace.servlet.enabled");
        }
    }

    @Test
    @DisplayName("rest-template.enabled=false 应正确解析")
    void load_withRestTemplateDisabled_shouldParseCorrectly() {
        System.setProperty("gray.trace.rest-template.enabled", "false");

        try {
            GrayProperties properties = AgentConfigLoader.load();

            assertNotNull(properties);
            assertFalse(properties.getRestTemplate().isEnabled());
        } finally {
            System.clearProperty("gray.trace.rest-template.enabled");
        }
    }

    @Test
    @DisplayName("thread-pool.enabled=false 应正确解析")
    void load_withThreadPoolDisabled_shouldParseCorrectly() {
        System.setProperty("gray.trace.thread-pool.enabled", "false");

        try {
            GrayProperties properties = AgentConfigLoader.load();

            assertNotNull(properties);
            assertFalse(properties.getThreadPool().isEnabled());
        } finally {
            System.clearProperty("gray.trace.thread-pool.enabled");
        }
    }

    @Test
    @DisplayName("mq.enabled=true 应正确解析")
    void load_withMqEnabled_shouldParseCorrectly() {
        System.setProperty("gray.trace.mq.enabled", "true");

        try {
            GrayProperties properties = AgentConfigLoader.load();

            assertNotNull(properties);
            assertTrue(properties.getMq().isEnabled());
        } finally {
            System.clearProperty("gray.trace.mq.enabled");
        }
    }

    @Test
    @DisplayName("completable-future.enabled=false 应正确解析")
    void load_withCompletableFutureDisabled_shouldParseCorrectly() {
        System.setProperty("gray.trace.completable-future.enabled", "false");

        try {
            GrayProperties properties = AgentConfigLoader.load();

            assertNotNull(properties);
            assertFalse(properties.getCompletableFuture().isEnabled());
        } finally {
            System.clearProperty("gray.trace.completable-future.enabled");
        }
    }

    @Test
    @DisplayName("默认日志开关关闭")
    void defaultLogDisabled() {
        GrayProperties props = AgentConfigLoader.load();
        assertFalse(props.getLog().isEnabled());
    }

    @Test
    @DisplayName("通过系统属性开启日志")
    void logEnabledViaSystemProperty() {
        System.setProperty("gray.trace.log.enabled", "true");
        try {
            GrayProperties props = AgentConfigLoader.load();
            assertTrue(props.getLog().isEnabled());
        } finally {
            System.clearProperty("gray.trace.log.enabled");
        }
    }
}