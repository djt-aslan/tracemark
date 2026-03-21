package io.tracemark.agent.advice;

import io.tracemark.gray.core.GrayConstants;
import io.tracemark.gray.core.GrayContext;
import net.bytebuddy.asm.Advice;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * Advice：拦截 javax.servlet.http.HttpServlet#service 方法
 *
 * <p>在方法进入时提取 {@code x-gray-tag} Header 写入上下文，
 * 方法退出时清理，防止线程池复用污染。
 */
public class ServletInboundAdvice {

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