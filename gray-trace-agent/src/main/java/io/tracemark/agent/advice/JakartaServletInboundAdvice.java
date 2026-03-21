package io.tracemark.agent.advice;

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
        GrayContext.set(tag != null && !tag.isEmpty() ? tag : GrayConstants.TAG_STABLE);
    }

    @Advice.OnMethodExit(onThrowable = Throwable.class)
    public static void onExit() {
        GrayContext.clear();
    }
}