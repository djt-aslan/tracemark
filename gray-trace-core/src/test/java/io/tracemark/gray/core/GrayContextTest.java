package io.tracemark.gray.core;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * GrayContext 单元测试
 */
class GrayContextTest {

    @BeforeEach
    @AfterEach
    void clearContext() {
        GrayContext.clear();
    }

    @Nested
    @DisplayName("set 方法测试")
    class SetTest {

        @Test
        @DisplayName("设置有效灰度标")
        void set_validTag_shouldStore() {
            GrayContext.set("gray-v1");
            assertEquals("gray-v1", GrayContext.get());
        }

        @Test
        @DisplayName("设置 null 应重置为 stable")
        void set_null_shouldResetToStable() {
            GrayContext.set("gray-v1");
            GrayContext.set(null);
            assertEquals(GrayConstants.TAG_STABLE, GrayContext.get());
        }

        @Test
        @DisplayName("设置空字符串应重置为 stable")
        void set_empty_shouldResetToStable() {
            GrayContext.set("");
            assertEquals(GrayConstants.TAG_STABLE, GrayContext.get());
        }
    }

    @Nested
    @DisplayName("get 方法测试")
    class GetTest {

        @Test
        @DisplayName("未设置时默认返回 stable")
        void get_default_shouldReturnStable() {
            assertEquals(GrayConstants.TAG_STABLE, GrayContext.get());
        }

        @Test
        @DisplayName("设置后返回正确值")
        void get_afterSet_shouldReturnSetValue() {
            GrayContext.set("gray-v2");
            assertEquals("gray-v2", GrayContext.get());
        }
    }

    @Nested
    @DisplayName("isGray 方法测试")
    class IsGrayTest {

        @Test
        @DisplayName("默认状态不是灰度")
        void isGray_default_shouldReturnFalse() {
            assertFalse(GrayContext.isGray());
        }

        @Test
        @DisplayName("设置为灰度标后返回 true")
        void isGray_withGrayTag_shouldReturnTrue() {
            GrayContext.set("gray-v1");
            assertTrue(GrayContext.isGray());
        }

        @Test
        @DisplayName("设置为 stable 后返回 false")
        void isGray_withStableTag_shouldReturnFalse() {
            GrayContext.set(GrayConstants.TAG_STABLE);
            assertFalse(GrayContext.isGray());
        }
    }

    @Nested
    @DisplayName("clear 方法测试")
    class ClearTest {

        @Test
        @DisplayName("清除后应恢复默认值")
        void clear_shouldRestoreDefault() {
            GrayContext.set("gray-v1");
            GrayContext.clear();
            assertEquals(GrayConstants.TAG_STABLE, GrayContext.get());
        }
    }

    @Nested
    @DisplayName("线程传递测试")
    class ThreadPropagationTest {

        @Test
        @DisplayName("子线程应继承父线程灰度标")
        void childThread_shouldInheritTag() throws Exception {
            GrayContext.set("gray-v1");
            Thread child = new Thread(() -> {
                assertEquals("gray-v1", GrayContext.get());
            });
            child.start();
            child.join();
        }
    }
}