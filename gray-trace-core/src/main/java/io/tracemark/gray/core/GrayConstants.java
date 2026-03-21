package io.tracemark.gray.core;

/**
 * 灰度追踪相关常量
 */
public final class GrayConstants {

    private GrayConstants() {}

    /** 灰度标签 HTTP Header 名称 */
    public static final String HEADER_GRAY_TAG = "x-gray-tag";

    /** 灰度标签 HTTP Header 名称（备用，小写兼容） */
    public static final String HEADER_GRAY_TAG_LOWER = "x-gray-tag";

    /** 稳定版标签值 */
    public static final String TAG_STABLE = "stable";

    /** MQ 消息属性 Key */
    public static final String MQ_PROPERTY_GRAY_TAG = "grayTag";

    /** 系统属性 Key（JVM启动参数 -D 传入） */
    public static final String SYS_PROP_ENABLED = "gray.trace.enabled";
}
