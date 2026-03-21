package io.tracemark.agent.advice;

import io.tracemark.gray.core.GrayConstants;
import io.tracemark.gray.core.GrayContext;
import net.bytebuddy.asm.Advice;
import okhttp3.Request;

/**
 * Advice：拦截 OkHttp RealCall，注入 x-gray-tag Header
 *
 * <p>插桩 {@code RealCall#execute} 和 {@code RealCall#enqueue}，
 * 通过替换 originalRequest 实现无侵入 Header 注入。
 */
public class OkHttpOutboundAdvice {

    @Advice.OnMethodEnter
    public static void onEnter(@Advice.FieldValue(value = "originalRequest", readOnly = false) Request request) {
        String tag = GrayContext.get();
        if (tag != null && !tag.isEmpty()
                && request.header(GrayConstants.HEADER_GRAY_TAG) == null) {
            request = request.newBuilder()
                    .header(GrayConstants.HEADER_GRAY_TAG, tag)
                    .build();
        }
    }
}