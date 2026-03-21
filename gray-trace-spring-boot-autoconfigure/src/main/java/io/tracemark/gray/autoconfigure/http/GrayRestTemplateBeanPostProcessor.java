package io.tracemark.gray.autoconfigure.http;

import io.tracemark.gray.core.GrayProperties;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.List;

/**
 * 自动为 Spring 容器中所有 {@link RestTemplate} Bean 注入灰度拦截器
 */
public class GrayRestTemplateBeanPostProcessor implements BeanPostProcessor {

    private final GrayProperties properties;

    public GrayRestTemplateBeanPostProcessor(GrayProperties properties) {
        this.properties = properties;
    }

    @Override
    public Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException {
        if (!(bean instanceof RestTemplate)) {
            return bean;
        }
        if (!properties.isEnabled() || !properties.getRestTemplate().isEnabled()) {
            return bean;
        }
        RestTemplate restTemplate = (RestTemplate) bean;

        // 避免重复注入
        boolean alreadyAdded = restTemplate.getInterceptors().stream()
                .anyMatch(i -> i instanceof GrayRestTemplateInterceptor);
        if (!alreadyAdded) {
            List<ClientHttpRequestInterceptor> interceptors = new ArrayList<>(restTemplate.getInterceptors());
            interceptors.add(0, new GrayRestTemplateInterceptor());  // 放在最前，最先执行
            restTemplate.setInterceptors(interceptors);
        }
        return bean;
    }
}