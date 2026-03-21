package io.tracemark.gray.autoconfigure.http;

import io.tracemark.gray.core.GrayProperties;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanPostProcessor;

import java.net.http.HttpClient;

/**
 * 自动将 Spring 容器中所有 {@link HttpClient} Bean 包装为 {@link GrayHttpClientWrapper}
 */
public class GrayHttpClientBeanPostProcessor implements BeanPostProcessor {

    private final GrayProperties properties;

    public GrayHttpClientBeanPostProcessor(GrayProperties properties) {
        this.properties = properties;
    }

    @Override
    public Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException {
        if (!(bean instanceof HttpClient)) {
            return bean;
        }
        if (!properties.isEnabled() || !properties.getHttpClient().isEnabled()) {
            return bean;
        }
        return GrayHttpClientWrapper.wrap((HttpClient) bean);
    }
}