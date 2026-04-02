package io.tracemark.gray.autoconfigure.http;

import io.tracemark.gray.core.GrayProperties;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClientBuilder;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanPostProcessor;

/**
 * 自动为 Spring 容器中所有 Apache HttpClient 5.x {@link CloseableHttpClient} Bean 注入灰度拦截器
 */
public class GrayApacheHttp5ClientBeanPostProcessor implements BeanPostProcessor {

    private final GrayProperties properties;

    public GrayApacheHttp5ClientBeanPostProcessor(GrayProperties properties) {
        this.properties = properties;
    }

    @Override
    public Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException {
        if (!(bean instanceof CloseableHttpClient)) {
            return bean;
        }
        if (!properties.isEnabled() || !properties.getApacheHttpClient().isEnabled()) {
            return bean;
        }
        return HttpClientBuilder.create()
                .addRequestInterceptorLast(new GrayApacheHttp5ClientInterceptor())
                .build();
    }
}
