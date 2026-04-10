package io.tracemark.agent;

import io.tracemark.agent.transformer.*;
import io.tracemark.gray.core.GrayProperties;
import net.bytebuddy.agent.builder.AgentBuilder;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.dynamic.DynamicType;
import net.bytebuddy.matcher.ElementMatchers;
import net.bytebuddy.utility.JavaModule;

import java.lang.instrument.Instrumentation;
import java.security.ProtectionDomain;
import java.util.logging.Logger;

/**
 * Java Agent 入口
 *
 * <p>通过 JVM 启动参数引入，零代码改动：
 * <pre>
 * java -javaagent:/path/to/gray-trace-agent.jar \
 *      -Dgray.trace.enabled=true \
 *      -Dgray.trace.mq.enabled=false \
 *      -jar your-service.jar
 * </pre>
 *
 * <p>支持的插桩目标：
 * <ul>
 *   <li>javax/jakarta Servlet：提取入口 Header → GrayContext</li>
 *   <li>RestTemplate：出口注入 Header</li>
 *   <li>OkHttp：出口注入 Header</li>
 *   <li>JDK HttpClient：出口注入 Header</li>
 *   <li>ThreadPoolExecutor：TTL 包装保证线程池上下文传递</li>
 *   <li>RocketMQ DefaultMQProducer：发送前注入消息属性</li>
 *   <li>Apache HttpClient 4.x/5.x：出口注入 Header</li>
 *   <li>CompletableFuture 异步方法：TTL 包装保证异步上下文传递</li>
 * </ul>
 */
public class GrayTraceAgent {

    private static final Logger log = Logger.getLogger(GrayTraceAgent.class.getName());

    /**
     * JVM 启动时调用（-javaagent）
     */
    public static void premain(String agentArgs, Instrumentation inst) {
        install(agentArgs, inst);
    }

    /**
     * 运行时动态 attach 调用（tools.jar / Attach API）
     */
    public static void agentmain(String agentArgs, Instrumentation inst) {
        install(agentArgs, inst);
    }

    private static void install(String agentArgs, Instrumentation inst) {
        GrayProperties config = AgentConfigLoader.load();
        GrayTraceLogger.init(config);

        if (!config.isEnabled()) {
            log.info("[GrayTrace] Agent disabled (gray.trace.enabled=false), skip instrumentation.");
            return;
        }

        log.info("[GrayTrace] Installing gray trace agent...");

        AgentBuilder builder = new AgentBuilder.Default()
                .with(AgentBuilder.RedefinitionStrategy.RETRANSFORMATION)
                .with(AgentBuilder.TypeStrategy.Default.REDEFINE)
                .with(new AgentBuilder.Listener.Adapter() {
                    @Override
                    public void onError(String typeName, ClassLoader classLoader,
                                        JavaModule module, boolean loaded, Throwable throwable) {
                        log.warning("[GrayTrace] Instrumentation failed for " + typeName + ": " + throwable.getMessage());
                    }
                });

        // 1. Servlet 入口（javax）
        if (config.getServlet().isEnabled()) {
            builder = builder.type(ElementMatchers.hasSuperType(ElementMatchers.named("javax.servlet.http.HttpServlet")))
                    .transform(new ServletInboundTransformer());

            // jakarta 版
            builder = builder.type(ElementMatchers.hasSuperType(ElementMatchers.named("jakarta.servlet.http.HttpServlet")))
                    .transform(new JakartaServletInboundTransformer());
        }

        // 2. RestTemplate 出口
        if (config.getRestTemplate().isEnabled()) {
            builder = builder.type(ElementMatchers.hasSuperType(ElementMatchers.named(
                            "org.springframework.http.client.AbstractClientHttpRequest")))
                    .transform(new RestTemplateOutboundTransformer());
        }

        // 3. OkHttp 出口
        if (config.getOkHttp().isEnabled()) {
            builder = builder.type(ElementMatchers.named("okhttp3.internal.connection.RealCall"))
                    .transform(new OkHttpOutboundTransformer());
        }

        // 4. ThreadPoolExecutor 上下文传递
        if (config.getThreadPool().isEnabled()) {
            builder = builder.type(ElementMatchers.isSubTypeOf(java.util.concurrent.ThreadPoolExecutor.class))
                    .transform(new ThreadPoolTransformer());
        }

        // 5. RocketMQ 生产者
        if (config.getMq().isEnabled() && config.getMq().isProducer()) {
            builder = builder.type(ElementMatchers.named(
                            "org.apache.rocketmq.client.impl.producer.DefaultMQProducerImpl"))
                    .transform(new RocketMqProducerTransformer());
        }

        // 6. Apache HttpClient 4.x 出口
        if (config.getApacheHttpClient().isEnabled()) {
            builder = builder.type(ElementMatchers.hasSuperType(ElementMatchers.named(
                            "org.apache.http.impl.client.CloseableHttpClient")))
                    .transform(new ApacheHttpClientOutboundTransformer());

            // 5.x 版本
            builder = builder.type(ElementMatchers.hasSuperType(ElementMatchers.named(
                            "org.apache.hc.client5.impl.classic.CloseableHttpClient")))
                    .transform(new ApacheHttp5ClientOutboundTransformer());
        }

        // 7. CompletableFuture 异步传递
        if (config.getCompletableFuture().isEnabled()) {
            builder = builder.type(ElementMatchers.named("java.util.concurrent.CompletableFuture"))
                    .transform(new CompletableFutureAsyncTransformer());
        }

        builder.installOn(inst);

        log.info("[GrayTrace] Agent installed successfully.");
    }
}