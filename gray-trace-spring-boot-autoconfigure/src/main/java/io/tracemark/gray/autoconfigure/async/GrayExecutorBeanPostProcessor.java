package io.tracemark.gray.autoconfigure.async;

import com.alibaba.ttl.TtlRunnable;
import com.alibaba.ttl.TtlCallable;
import com.alibaba.ttl.threadpool.TtlExecutors;
import io.tracemark.gray.core.GrayProperties;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.core.task.TaskDecorator;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;

/**
 * 线程池灰度上下文传递 BeanPostProcessor
 *
 * <p>对 Spring 容器中的线程池 Bean 进行后处理：
 * <ul>
 *   <li>{@link ThreadPoolTaskExecutor}：设置 {@link GrayTaskDecorator}（链式追加，不覆盖已有的）</li>
 *   <li>普通 {@link ExecutorService}：使用 TTL 包装，自动在任务提交/执行时传递上下文</li>
 * </ul>
 *
 * <p><b>注意：</b>{@code ThreadPoolTaskExecutor} 的 TaskDecorator 必须在 {@code initialize()} 之前设置，
 * 本处理器在 {@code postProcessBeforeInitialization} 阶段注入，时机正确。
 */
public class GrayExecutorBeanPostProcessor implements BeanPostProcessor {

    private final GrayProperties properties;

    public GrayExecutorBeanPostProcessor(GrayProperties properties) {
        this.properties = properties;
    }

    @Override
    public Object postProcessBeforeInitialization(Object bean, String beanName) throws BeansException {
        if (!properties.isEnabled() || !properties.getThreadPool().isEnabled()) {
            return bean;
        }

        // ThreadPoolTaskExecutor：在初始化前设置 TaskDecorator
        if (bean instanceof ThreadPoolTaskExecutor && properties.getThreadPool().isAsyncDecorator()) {
            ThreadPoolTaskExecutor executor = (ThreadPoolTaskExecutor) bean;
            TaskDecorator existing = getExistingDecorator(executor);
            if (existing instanceof GrayTaskDecorator) {
                return bean;  // 已处理，跳过
            }
            GrayTaskDecorator grayDecorator = new GrayTaskDecorator();
            // 链式组合：已有 decorator 先执行，gray decorator 套在外层
            if (existing != null) {
                executor.setTaskDecorator(runnable -> grayDecorator.decorate(existing.decorate(runnable)));
            } else {
                executor.setTaskDecorator(grayDecorator);
            }
        }
        return bean;
    }

    @Override
    public Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException {
        if (!properties.isEnabled() || !properties.getThreadPool().isEnabled()) {
            return bean;
        }

        // 普通 ExecutorService（非 ThreadPoolTaskExecutor）：TTL 包装
        if (bean instanceof ExecutorService && !(bean instanceof ThreadPoolTaskExecutor)) {
            ExecutorService wrapped = TtlExecutors.getTtlExecutorService((ExecutorService) bean);
            if (wrapped != bean) {
                return wrapped;
            }
        }

        return bean;
    }

    private TaskDecorator getExistingDecorator(ThreadPoolTaskExecutor executor) {
        try {
            java.lang.reflect.Field field = ThreadPoolTaskExecutor.class.getDeclaredField("taskDecorator");
            field.setAccessible(true);
            return (TaskDecorator) field.get(executor);
        } catch (Exception e) {
            return null;
        }
    }
}