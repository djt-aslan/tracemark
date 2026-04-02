package io.tracemark.gray.core;

/**
 * 灰度追踪配置属性（POJO，供 Starter 和 Agent 共用）
 *
 * <p>Spring Boot Starter 通过 {@code @ConfigurationProperties} 注入；
 * Java Agent 通过读取 {@code -D} 系统属性初始化。
 */
public class GrayProperties {

    /** 全局总开关，false 时所有拦截均不生效，默认 true */
    private boolean enabled = true;

    /** Servlet 入口过滤器（提取 x-gray-tag 请求头） */
    private Servlet servlet = new Servlet();

    /** RestTemplate 出口拦截 */
    private RestTemplate restTemplate = new RestTemplate();

    /** OkHttp 出口拦截 */
    private OkHttp okHttp = new OkHttp();

    /** JDK HttpClient 出口拦截 */
    private HttpClient httpClient = new HttpClient();

    /** OpenFeign 出口拦截 */
    private Feign feign = new Feign();

    /** 线程池上下文传递（ThreadPoolExecutor / @Async） */
    private ThreadPool threadPool = new ThreadPool();

    /** 消息队列染色传递 */
    private Mq mq = new Mq();

    /** WebFlux/WebClient 支持 */
    private WebFlux webFlux = new WebFlux();

    /** Apache HttpClient 出口拦截 */
    private ApacheHttpClient apacheHttpClient = new ApacheHttpClient();

    // ===================== 嵌套配置 =====================

    public static class Servlet {
        /** 是否启用 Servlet Filter 提取灰度头，默认 true */
        private boolean enabled = true;
        public boolean isEnabled() { return enabled; }
        public void setEnabled(boolean enabled) { this.enabled = enabled; }
    }

    public static class RestTemplate {
        /** 是否拦截 RestTemplate 出口并透传灰度头，默认 true */
        private boolean enabled = true;
        public boolean isEnabled() { return enabled; }
        public void setEnabled(boolean enabled) { this.enabled = enabled; }
    }

    public static class OkHttp {
        /** 是否拦截 OkHttp 出口并透传灰度头，默认 true */
        private boolean enabled = true;
        public boolean isEnabled() { return enabled; }
        public void setEnabled(boolean enabled) { this.enabled = enabled; }
    }

    public static class HttpClient {
        /** 是否拦截 JDK HttpClient 出口并透传灰度头，默认 true */
        private boolean enabled = true;
        public boolean isEnabled() { return enabled; }
        public void setEnabled(boolean enabled) { this.enabled = enabled; }
    }

    public static class Feign {
        /** 是否拦截 Feign 出口并透传灰度头，默认 true */
        private boolean enabled = true;
        public boolean isEnabled() { return enabled; }
        public void setEnabled(boolean enabled) { this.enabled = enabled; }
    }

    public static class ThreadPool {
        /**
         * 是否开启线程池上下文传递，默认 true
         * 开启后通过 TTL Executor 包装，保证灰度标在线程池中不丢失
         */
        private boolean enabled = true;

        /**
         * 是否对 Spring @Async 线程池设置 TaskDecorator，默认 true
         * 需要 enabled=true 才生效
         */
        private boolean asyncDecorator = true;

        public boolean isEnabled() { return enabled; }
        public void setEnabled(boolean enabled) { this.enabled = enabled; }
        public boolean isAsyncDecorator() { return asyncDecorator; }
        public void setAsyncDecorator(boolean asyncDecorator) { this.asyncDecorator = asyncDecorator; }
    }

    public static class Mq {
        /**
         * MQ 整体开关，默认 false（按需开启）
         * 主要考虑异步消息语义与同步灰度语义差异，保守默认关闭
         */
        private boolean enabled = false;

        /** 生产者是否注入灰度头到消息属性，默认 true（依赖 mq.enabled） */
        private boolean producer = true;

        /** 消费者是否从消息属性恢复灰度上下文，默认 true（依赖 mq.enabled） */
        private boolean consumer = true;

        public boolean isEnabled() { return enabled; }
        public void setEnabled(boolean enabled) { this.enabled = enabled; }
        public boolean isProducer() { return producer; }
        public void setProducer(boolean producer) { this.producer = producer; }
        public boolean isConsumer() { return consumer; }
        public void setConsumer(boolean consumer) { this.consumer = consumer; }
    }

    public static class WebFlux {
        /** 是否开启 WebFlux/WebClient 灰度传递，默认 true */
        private boolean enabled = true;
        public boolean isEnabled() { return enabled; }
        public void setEnabled(boolean enabled) { this.enabled = enabled; }
    }

    public static class ApacheHttpClient {
        /** 是否拦截 Apache HttpClient 出口并透传灰度头，默认 true */
        private boolean enabled = true;
        public boolean isEnabled() { return enabled; }
        public void setEnabled(boolean enabled) { this.enabled = enabled; }
    }

    // ===================== Getters / Setters =====================

    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }

    public Servlet getServlet() { return servlet; }
    public void setServlet(Servlet servlet) { this.servlet = servlet; }

    public RestTemplate getRestTemplate() { return restTemplate; }
    public void setRestTemplate(RestTemplate restTemplate) { this.restTemplate = restTemplate; }

    public OkHttp getOkHttp() { return okHttp; }
    public void setOkHttp(OkHttp okHttp) { this.okHttp = okHttp; }

    public HttpClient getHttpClient() { return httpClient; }
    public void setHttpClient(HttpClient httpClient) { this.httpClient = httpClient; }

    public Feign getFeign() { return feign; }
    public void setFeign(Feign feign) { this.feign = feign; }

    public ThreadPool getThreadPool() { return threadPool; }
    public void setThreadPool(ThreadPool threadPool) { this.threadPool = threadPool; }

    public Mq getMq() { return mq; }
    public void setMq(Mq mq) { this.mq = mq; }

    public WebFlux getWebFlux() { return webFlux; }
    public void setWebFlux(WebFlux webFlux) { this.webFlux = webFlux; }

    public ApacheHttpClient getApacheHttpClient() { return apacheHttpClient; }
    public void setApacheHttpClient(ApacheHttpClient apacheHttpClient) { this.apacheHttpClient = apacheHttpClient; }
}
