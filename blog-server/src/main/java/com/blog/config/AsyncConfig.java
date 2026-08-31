package com.blog.config;

import org.springframework.aop.interceptor.AsyncUncaughtExceptionHandler;
import org.springframework.aop.interceptor.SimpleAsyncUncaughtExceptionHandler;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.AsyncConfigurer;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;

/**
 * 异步任务线程池 — 邮件发送专用。
 *
 * <p>默认不提供 Bean 时,Spring 会用 SimpleAsyncTaskExecutor 每任务开新线程,不可控。
 * 这里给一个命名线程池便于排障,队列上限 200 防止内存爆,Rejected 直接丢弃 + log,
 * 评论通知是非关键路径,宁可丢不可拖垮请求线程。</p>
 */
@Configuration
@EnableAsync
public class AsyncConfig implements AsyncConfigurer {

    @Bean(name = "mailTaskExecutor")
    public Executor mailTaskExecutor() {
        ThreadPoolTaskExecutor exec = new ThreadPoolTaskExecutor();
        exec.setCorePoolSize(2);
        exec.setMaxPoolSize(8);
        exec.setQueueCapacity(200);
        exec.setThreadNamePrefix("mail-async-");
        exec.setRejectedExecutionHandler((r, e) -> {
            // 队列满:丢弃 + 警告。业务侧会在 catch 里再 log,这里只兜底
            org.slf4j.LoggerFactory.getLogger(AsyncConfig.class)
                    .warn("mailTaskExecutor queue full, dropping task");
        });
        // 启动时初始化,否则首次提交会延迟启动线程
        exec.initialize();
        return exec;
    }

    @Bean(name = "visitTaskExecutor")
    public Executor visitTaskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(1);
        executor.setMaxPoolSize(2);
        executor.setQueueCapacity(200);
        executor.setThreadNamePrefix("visit-async-");
        executor.setRejectedExecutionHandler((r, e) -> org.slf4j.LoggerFactory.getLogger(AsyncConfig.class)
                .warn("访问统计队列已满，丢弃本次记录"));
        executor.initialize();
        return executor;
    }

    @Override
    public AsyncUncaughtExceptionHandler getAsyncUncaughtExceptionHandler() {
        return new SimpleAsyncUncaughtExceptionHandler();
    }
}