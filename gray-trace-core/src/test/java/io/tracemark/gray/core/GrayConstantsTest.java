package io.tracemark.gray.core;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * GrayConstants 常量验证测试
 */
class GrayConstantsTest {

    @Test
    @DisplayName("HTTP Header 名称应为 x-gray-tag")
    void headerGrayTag_shouldBeCorrect() {
        assertEquals("x-gray-tag", GrayConstants.HEADER_GRAY_TAG);
    }

    @Test
    @DisplayName("备用 Header 名称应一致")
    void headerGrayTagLower_shouldMatch() {
        assertEquals(GrayConstants.HEADER_GRAY_TAG, GrayConstants.HEADER_GRAY_TAG_LOWER);
    }

    @Test
    @DisplayName("稳定版标签应为 stable")
    void tagStable_shouldBeStable() {
        assertEquals("stable", GrayConstants.TAG_STABLE);
    }

    @Test
    @DisplayName("MQ 属性 Key 应为 grayTag")
    void mqPropertyKey_shouldBeCorrect() {
        assertEquals("grayTag", GrayConstants.MQ_PROPERTY_GRAY_TAG);
    }

    @Test
    @DisplayName("系统属性 Key 应为 gray.trace.enabled")
    void sysPropEnabled_shouldBeCorrect() {
        assertEquals("gray.trace.enabled", GrayConstants.SYS_PROP_ENABLED);
    }
}