package io.tracemark.agent.advice;

import io.tracemark.agent.GrayTraceLogger;
import io.tracemark.gray.core.GrayConstants;
import io.tracemark.gray.core.GrayContext;
import net.bytebuddy.asm.Advice;

import jakarta.servlet.http.HttpServletRequest;

/**
 * Advice：拦截 jakarta.servlet.http.HttpServlet#service 方法（Spring Boot 3.x）
 */
public class JakartaServletInboundAdvice {

    @Advice.OnMethodEnter
    public static void onEnter(@Advice.Argument(0) HttpServletRequest request) {
        String tag = request.getHeader(GrayConstants.HEADER_GRAY_TAG);
        String effectiveTag = tag != null && !tag.isEmpty() ? tag : GrayConstants.TAG_STABLE;
        GrayContext.set(effectiveTag);

        // 日志输出
        GrayTraceLogger.logInbound(effectiveTag, request.getRequestURI(), Thread.currentThread().getName());
    }

    @Advice.OnMethodExit(onThrowable = Throwable.class)
    public static void onExit() {
        String tag = GrayContext.get();
        GrayTraceLogger.logClear(tag, Thread.currentThread().getName());
        GrayContext.clear();
    }
}