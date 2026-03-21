package io.tracemark.agent;

import io.tracemark.gray.core.GrayProperties;

/**
 * 从 JVM 系统属性（-D 参数）加载 Agent 配置
 *
 * <p>Agent 场景下无 Spring，通过读取 System Properties 初始化 {@link GrayProperties}。
 *
 * <p>支持的参数：
 * <pre>
 * -Dgray.trace.enabled=true
 * -Dgray.trace.servlet.enabled=true
 * -Dgray.trace.rest-template.enabled=true
 * -Dgray.trace.ok-http.enabled=true
 * -Dgray.trace.http-client.enabled=true
 * -Dgray.trace.thread-pool.enabled=true
 * -Dgray.trace.mq.enabled=false
 * -Dgray.trace.mq.producer=true
 * -Dgray.trace.mq.consumer=true
 * </pre>
 */
public class AgentConfigLoader {

    public static GrayProperties load() {
        GrayProperties props = new GrayProperties();

        props.setEnabled(getBool("gray.trace.enabled", true));
        props.getServlet().setEnabled(getBool("gray.trace.servlet.enabled", true));
        props.getRestTemplate().setEnabled(getBool("gray.trace.rest-template.enabled", true));
        props.getOkHttp().setEnabled(getBool("gray.trace.ok-http.enabled", true));
        props.getHttpClient().setEnabled(getBool("gray.trace.http-client.enabled", true));
        props.getFeign().setEnabled(getBool("gray.trace.feign.enabled", true));

        props.getThreadPool().setEnabled(getBool("gray.trace.thread-pool.enabled", true));
        props.getThreadPool().setAsyncDecorator(getBool("gray.trace.thread-pool.async-decorator", true));

        props.getMq().setEnabled(getBool("gray.trace.mq.enabled", false));
        props.getMq().setProducer(getBool("gray.trace.mq.producer", true));
        props.getMq().setConsumer(getBool("gray.trace.mq.consumer", true));

        return props;
    }

    private static boolean getBool(String key, boolean defaultValue) {
        String val = System.getProperty(key);
        if (val == null || val.isEmpty()) {
            return defaultValue;
        }
        return "true".equalsIgnoreCase(val.trim());
    }
}