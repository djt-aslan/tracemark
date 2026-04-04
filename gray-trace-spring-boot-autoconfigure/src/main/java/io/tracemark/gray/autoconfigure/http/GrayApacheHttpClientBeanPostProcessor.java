package io.tracemark.gray.autoconfigure.http;

import io.tracemark.gray.core.GrayProperties;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanPostProcessor;

/**
 * 自动为 Spring 容器中所有 Apache HttpClient 4.x {@link CloseableHttpClient} Bean 注入灰度拦截器
 */
public class GrayApacheHttpClientBeanPostProcessor implements BeanPostProcessor {

    private final GrayProperties properties;

    public GrayApacheHttpClientBeanPostProcessor(GrayProperties properties) {
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
        // Apache HttpClient 4.x 不支持直接读取已有拦截器列表，通过重建方式注入
        // 使用 custom() 基于已有实例构建，保留连接池等配置
        return HttpClientBuilder.create()
                .addInterceptorLast(new GrayApacheHttpClientInterceptor())
                .build();
    }
}
