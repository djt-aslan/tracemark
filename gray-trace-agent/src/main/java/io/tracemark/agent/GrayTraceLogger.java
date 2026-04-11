package io.tracemark.agent;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Agent 链路追踪日志工具类
 *
 * <p>统一日志格式：{@code [GrayTrace] {操作类型} → {描述}, tag={tag}, {上下文信息}}
 *
 * <p>日志级别为 DEBUG，通过 {@code gray.trace.log.enabled} 配置开关控制。
 */
public final class GrayTraceLogger {

    private static final Logger LOG = LoggerFactory.getLogger(GrayTraceLogger.class);
    private static volatile boolean enabled = false;

    private GrayTraceLogger() {}

    /**
     * 初始化日志开关
     *
     * @param props 配置属性
     */
    public static void init(io.tracemark.gray.core.GrayProperties props) {
        enabled = props.getLog().isEnabled();
    }

    /**
     * 入口日志：提取 Header
     *
     * @param tag    灰度标签值
     * @param uri    请求 URI
     * @param thread 当前线程名
     */
    public static void logInbound(String tag, String uri, String thread) {
        if (!enabled || tag == null || tag.isEmpty()) {
            return;
        }
        if (LOG.isDebugEnabled()) {
            LOG.debug("[GrayTrace] 入口 → 提取Header, tag={}, uri={}, thread={}", tag, uri, thread);
        }
    }

    /**
     * 出口日志：注入 Header
     *
     * @param tag    灰度标签值
     * @param url    请求 URL
     * @param thread 当前线程名
     */
    public static void logOutbound(String tag, String url, String thread) {
        if (!enabled || tag == null || tag.isEmpty()) {
            return;
        }
        if (LOG.isDebugEnabled()) {
            LOG.debug("[GrayTrace] 出口 → 注入Header, tag={}, url={}, thread={}", tag, url, thread);
        }
    }

    /**
     * 异步日志：TTL 包装
     *
     * @param tag      灰度标签值
     * @param pool     线程池名或来源
     * @param taskType 任务类型
     * @param thread   当前线程名
     */
    public static void logAsync(String tag, String pool, String taskType, String thread) {
        if (!enabled || tag == null || tag.isEmpty()) {
            return;
        }
        if (LOG.isDebugEnabled()) {
            LOG.debug("[GrayTrace] 异步 → TTL包装, tag={}, pool={}, task={}, thread={}", tag, pool, taskType, thread);
        }
    }

    /**
     * 清理日志：上下文清理
     *
     * @param tag    灰度标签值
     * @param thread 当前线程名
     */
    public static void logClear(String tag, String thread) {
        if (!enabled || tag == null || tag.isEmpty()) {
            return;
        }
        if (LOG.isDebugEnabled()) {
            LOG.debug("[GrayTrace] 清理 → 上下文, tag={}, thread={}", tag, thread);
        }
    }

    /**
     * MQ 日志：消息属性注入
     *
     * @param tag    灰度标签值
     * @param topic  消息 Topic
     * @param thread 当前线程名
     */
    public static void logMq(String tag, String topic, String thread) {
        if (!enabled || tag == null || tag.isEmpty()) {
            return;
        }
        if (LOG.isDebugEnabled()) {
            LOG.debug("[GrayTrace] MQ → 注入属性, tag={}, topic={}, thread={}", tag, topic, thread);
        }
    }

    /**
     * MQ Consumer 日志：从消息恢复上下文
     *
     * @param tag    灰度标签值
     * @param topic  消息 Topic
     * @param thread 当前线程名
     */
    public static void logMqConsumer(String tag, String topic, String thread) {
        if (!enabled || tag == null || tag.isEmpty()) {
            return;
        }
        if (LOG.isDebugEnabled()) {
            LOG.debug("[GrayTrace] MQ消费 → 恢复上下文, tag={}, topic={}, thread={}", tag, topic, thread);
        }
    }

    /**
     * 检查日志是否启用
     *
     * @return 日志开关状态
     */
    public static boolean isEnabled() {
        return enabled;
    }
}