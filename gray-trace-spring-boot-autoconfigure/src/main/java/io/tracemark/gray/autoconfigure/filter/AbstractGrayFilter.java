package io.tracemark.gray.autoconfigure.filter;

import io.tracemark.gray.core.GrayConstants;
import io.tracemark.gray.core.GrayContext;

import java.io.IOException;

/**
 * 灰度入口过滤逻辑（与 Servlet API 版本无关的抽象）
 *
 * <p>实际的 javax / jakarta 版 Filter 各自继承此类，只负责提取 Header 和清理上下文。
 */
public abstract class AbstractGrayFilter {

    protected void doGrayFilter(String grayTagHeader, FilterChainInvoker chain) throws IOException, Exception {
        try {
            GrayContext.set(grayTagHeader);
            chain.doFilter();
        } finally {
            // 请求结束后清除，防止线程池复用时污染下一请求
            GrayContext.clear();
        }
    }

    protected String resolveTag(String headerValue) {
        if (headerValue == null || headerValue.isEmpty()) {
            return GrayConstants.TAG_STABLE;
        }
        return headerValue;
    }

    @FunctionalInterface
    public interface FilterChainInvoker {
        void doFilter() throws IOException, Exception;
    }
}