package io.tracemark.agent.advice;

import io.tracemark.agent.GrayTraceLogger;
import io.tracemark.gray.core.GrayConstants;
import io.tracemark.gray.core.GrayContext;
import net.bytebuddy.asm.Advice;
import org.springframework.http.HttpRequest;

/**
 * Advice：拦截 RestTemplate 内部 executeInternal，注入 x-gray-tag Header
 */
public class RestTemplateOutboundAdvice {

    @Advice.OnMethodEnter
    public static void onEnter(@Advice.Argument(0) HttpRequest request) {
        String tag = GrayContext.get();
        if (tag != null && !tag.isEmpty()) {
            request.getHeaders().set(GrayConstants.HEADER_GRAY_TAG, tag);

            // 日志输出
            GrayTraceLogger.logOutbound(tag, request.getURI().toString(), Thread.currentThread().getName());
        }
    }
}