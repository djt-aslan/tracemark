package io.tracemark.gray.autoconfigure.async;

import io.tracemark.gray.core.GrayContext;
import org.springframework.core.task.TaskDecorator;

/**
 * Spring {@code @Async} / {@link org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor}
 * 灰度上下文传递装饰器
 *
 * <p>工作原理：
 * <ol>
 *   <li>任务<b>提交时</b>：在调用者线程捕获当前灰度标签</li>
 *   <li>任务<b>执行时</b>：在工作线程还原灰度标签</li>
 *   <li>任务<b>完成时</b>：清理工作线程上下文，防止线程池复用污染</li>
 * </ol>
 */
public class GrayTaskDecorator implements TaskDecorator {

    @Override
    public Runnable decorate(Runnable runnable) {
        // ① 在提交线程（父线程）捕获灰度标
        String capturedTag = GrayContext.get();

        return () -> {
            // ② 在工作线程（子线程）还原
            String previousTag = GrayContext.get();
            try {
                GrayContext.set(capturedTag);
                runnable.run();
            } finally {
                // ③ 恢复工作线程原有状态，而非直接清除
                // 避免嵌套调用时破坏外层上下文
                GrayContext.set(previousTag);
            }
        };
    }
}