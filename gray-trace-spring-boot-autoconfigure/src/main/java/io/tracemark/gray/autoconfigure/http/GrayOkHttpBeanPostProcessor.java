package io.tracemark.gray.autoconfigure.http;

import io.tracemark.gray.core.GrayProperties;
import okhttp3.OkHttpClient;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanPostProcessor;

/**
 * 自动为 Spring 容器中所有 {@link OkHttpClient} Bean 注入灰度拦截器
 */
public class GrayOkHttpBeanPostProcessor implements BeanPostProcessor {

    private final GrayProperties properties;

    public GrayOkHttpBeanPostProcessor(GrayProperties properties) {
        this.properties = properties;
    }

    @Override
    public Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException {
        if (!(bean instanceof OkHttpClient)) {
            return bean;
        }
        if (!properties.isEnabled() || !properties.getOkHttp().isEnabled()) {
            return bean;
        }
        OkHttpClient client = (OkHttpClient) bean;

        // 避免重复注入
        boolean alreadyAdded = client.interceptors().stream()
                .anyMatch(i -> i instanceof GrayOkHttpInterceptor);
        if (alreadyAdded) {
            return bean;
        }

        // OkHttpClient 是不可变的，通过 newBuilder() 重建
        return client.newBuilder()
                .addInterceptor(new GrayOkHttpInterceptor())
                .build();
    }
}