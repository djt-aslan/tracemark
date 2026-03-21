package io.tracemark.gray.core;

import com.alibaba.ttl.TransmittableThreadLocal;

/**
 * 灰度上下文持有器
 *
 * <p>使用 {@link TransmittableThreadLocal} 保证灰度标在以下场景下不丢失：
 * <ul>
 *   <li>普通子线程（InheritableThreadLocal 可处理）</li>
 *   <li>线程池复用场景（TTL 在任务提交时捕获、执行时恢复）</li>
 *   <li>{@code @Async} / {@code CompletableFuture.supplyAsync()}（需配合 TaskDecorator 或 TTL Executor）</li>
 * </ul>
 */
public final class GrayContext {

    private GrayContext() {}

    private static final TransmittableThreadLocal<String> GRAY_TAG =
            new TransmittableThreadLocal<String>() {
                @Override
                protected String initialValue() {
                    return GrayConstants.TAG_STABLE;
                }
            };

    /**
     * 设置当前线程灰度标签
     *
     * @param tag 灰度标签值，如 "gray-v1"；{@code null} 时重置为 stable
     */
    public static void set(String tag) {
        if (tag == null || tag.isEmpty()) {
            GRAY_TAG.set(GrayConstants.TAG_STABLE);
        } else {
            GRAY_TAG.set(tag);
        }
    }

    /**
     * 获取当前线程灰度标签，默认返回 {@code "stable"}
     */
    public static String get() {
        return GRAY_TAG.get();
    }

    /**
     * 判断当前请求是否为灰度请求（非 stable）
     */
    public static boolean isGray() {
        return !GrayConstants.TAG_STABLE.equals(GRAY_TAG.get());
    }

    /**
     * 清除当前线程灰度标签，防止线程池复用时上下文污染
     * <p>务必在请求结束时（finally 块）调用</p>
     */
    public static void clear() {
        GRAY_TAG.remove();
    }
}
