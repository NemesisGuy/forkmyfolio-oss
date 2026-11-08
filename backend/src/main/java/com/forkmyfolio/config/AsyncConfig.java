package com.forkmyfolio.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;

@Configuration
@EnableAsync
public class AsyncConfig {

        private static final org.slf4j.Logger log =
                        org.slf4j.LoggerFactory.getLogger(AsyncConfig.class);

    @Bean(name = "applicationTaskExecutor")
    public Executor applicationTaskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(2);
        executor.setMaxPoolSize(8);
        executor.setQueueCapacity(200);
        executor.setThreadNamePrefix("fmf-async-");
        // Phase 0.20: the DEFAULT rejection policy (Abort) throws synchronously
        // into the caller's request thread — under burst load full async queues
        // turned every viewer request into a 500. Visitor-stat updates are
        // best-effort: log and drop instead of failing the view.
        executor.setRejectedExecutionHandler((task, executor_) -> {
                log.warn("Async executor saturated (2-8 threads, queue 200) — dropped one background task "
                                + "(e.g. a visitor-stat increment). Request NOT affected. "
                                + "Under sustained stress this is a hint to buffer or batch stats.");
        });
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(30);
        executor.initialize();
        return executor;
    }
}
