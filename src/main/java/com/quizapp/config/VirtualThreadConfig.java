package com.quizapp.config;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.AsyncTaskExecutor;
import org.springframework.core.task.support.TaskExecutorAdapter;
import org.springframework.scheduling.annotation.EnableAsync;

@Configuration
@EnableAsync
public class VirtualThreadConfig {

    public static final String VIRTUAL_EXECUTOR = "virtualExecutor";

    @Bean(name = VIRTUAL_EXECUTOR, destroyMethod = "close")
    ExecutorService virtualExecutor() {
        return Executors.newVirtualThreadPerTaskExecutor();
    }

    /** Used by Spring MVC async + @Async so request work rides virtual threads. */
    @Bean
    AsyncTaskExecutor applicationTaskExecutor(ExecutorService virtualExecutor) {
        return new TaskExecutorAdapter(virtualExecutor);
    }
}
