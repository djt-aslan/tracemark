package io.tracemark.test.config;

import okhttp3.OkHttpClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.web.client.RestTemplate;

import java.util.concurrent.TimeUnit;

/**
 * 测试服务 Bean 配置
 *
 * <p>注册为 Spring Bean 后，GrayAutoConfiguration 中的 BeanPostProcessor 会自动拦截并注入灰度拦截器，
 * 业务代码无需任何修改。
 */
@Configuration
public class AppConfig {

    /**
     * RestTemplate Bean
     * → GrayRestTemplateBeanPostProcessor 自动注入 GrayRestTemplateInterceptor
     */
    @Bean
    public RestTemplate restTemplate() {
        return new RestTemplate();
    }

    /**
     * OkHttpClient Bean
     * → GrayOkHttpBeanPostProcessor 自动重建并注入 GrayOkHttpInterceptor
     */
    @Bean
    public OkHttpClient okHttpClient() {
        return new OkHttpClient.Builder()
                .connectTimeout(5, TimeUnit.SECONDS)
                .readTimeout(10, TimeUnit.SECONDS)
                .build();
    }

    /**
     * 自定义 @Async 线程池 Bean
     * → GrayExecutorBeanPostProcessor 在 BeforeInitialization 阶段设置 GrayTaskDecorator
     */
    @Bean(name = "grayTestAsyncExecutor")
    public ThreadPoolTaskExecutor asyncExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(4);
        executor.setMaxPoolSize(8);
        executor.setQueueCapacity(100);
        executor.setThreadNamePrefix("gray-async-");
        // 注意：TaskDecorator 由 GrayExecutorBeanPostProcessor 在 initialize() 前自动注入，
        // 这里无需手动设置，体现"无侵入"
        return executor;
    }
}