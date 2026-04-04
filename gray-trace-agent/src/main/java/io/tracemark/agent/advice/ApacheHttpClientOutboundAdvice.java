package io.tracemark.agent.advice;

import io.tracemark.gray.core.GrayConstants;
import io.tracemark.gray.core.GrayContext;
import net.bytebuddy.asm.Advice;
import org.apache.http.HttpRequest;

/**
 * Advice：拦截 Apache HttpClient 4.x CloseableHttpClient#execute，注入 x-gray-tag Header
 *
 * <p>{@code execute} 有多个重载签名（首参可能是 HttpUriRequest 或 HttpHost），
 * 使用 {@code @Advice.AllArguments} 遍历参数，以 {@code instanceof HttpRequest} 定位请求对象，
 * 兼容全部重载，失败静默跳过，不抛异常。
 */
public class ApacheHttpClientOutboundAdvice {

    @Advice.OnMethodEnter
    public static void onEnter(@Advice.AllArguments Object[] args) {
        if (args == null) return;
        for (Object arg : args) {
            if (arg instanceof HttpRequest) {
                HttpRequest request = (HttpRequest) arg;
                if (GrayContext.isGray()
                        && !request.containsHeader(GrayConstants.HEADER_GRAY_TAG)) {
                    request.setHeader(GrayConstants.HEADER_GRAY_TAG, GrayContext.get());
                }
                return;
            }
        }
    }
}
