package io.tracemark.agent.advice;

import io.tracemark.agent.GrayTraceLogger;
import io.tracemark.gray.core.GrayConstants;
import io.tracemark.gray.core.GrayContext;
import net.bytebuddy.asm.Advice;

import java.lang.reflect.Method;
import java.util.Optional;

/**
 * Advice：拦截 java.net.http.HttpClient 的 send/sendAsync 方法，
 * 在请求发出前自动注入 x-gray-tag Header。
 *
 * <p>由于 HttpRequest 是不可变对象，通过重建 Request 实现 Header 注入。
 * 由于 java.net.http 是 Java 11+ API，使用反射避免编译时依赖。
 *
 * <p>插桩目标：
 * <ul>
 *   <li>{@code HttpClient#send(HttpRequest, BodyHandler)} - 同步发送</li>
 *   <li>{@code HttpClient#sendAsync(HttpRequest, BodyHandler)} - 异步发送</li>
 * </ul>
 */
public class JdkHttpClientOutboundAdvice {

    private static final String HEADER_GRAY_TAG = GrayConstants.HEADER_GRAY_TAG;

    @Advice.OnMethodEnter
    public static void onEnter(
            @Advice.Argument(value = 0, readOnly = false) Object request) {
        String tag = GrayContext.get();
        if (tag == null || tag.isEmpty()) {
            return;
        }

        try {
            // 检查是否已有 gray tag header
            Object headers = invokeMethod(request, "headers");
            Optional<?> headerValue = (Optional<?>) invokeMethod(headers, "firstValue", HEADER_GRAY_TAG);
            if (headerValue.isPresent()) {
                return; // 已有 header，不覆盖
            }

            // 重建 request 并注入 header
            Object requestBuilder = invokeStaticMethod(
                    request.getClass(), "newBuilder", request);
            invokeMethod(requestBuilder, "header", HEADER_GRAY_TAG, tag);
            request = invokeMethod(requestBuilder, "build");

            // 获取 URI 用于日志
            Object uri = invokeMethod(request, "uri");
            String uriString = uri.toString();

            // 日志输出
            GrayTraceLogger.logOutbound(tag, uriString, Thread.currentThread().getName());
        } catch (Exception e) {
            // 反射失败时静默忽略，不影响业务
        }
    }

    private static Object invokeMethod(Object target, String methodName, Object... args)
            throws Exception {
        Class<?>[] paramTypes = new Class<?>[args.length];
        for (int i = 0; i < args.length; i++) {
            paramTypes[i] = args[i].getClass();
        }
        Method method = target.getClass().getMethod(methodName, paramTypes);
        return method.invoke(target, args);
    }

    private static Object invokeStaticMethod(Class<?> clazz, String methodName, Object... args)
            throws Exception {
        Class<?>[] paramTypes = new Class<?>[args.length];
        for (int i = 0; i < args.length; i++) {
            paramTypes[i] = args[i].getClass();
        }
        Method method = clazz.getMethod(methodName, paramTypes);
        return method.invoke(null, args);
    }
}
