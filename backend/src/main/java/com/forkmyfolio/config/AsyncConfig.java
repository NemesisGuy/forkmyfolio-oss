package com.forkmyfolio.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.AsyncConfigurer;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;

/**
 * Central configuration for the application's background-execution pool.
 *
 * <p>
 * <b>Phase 0.21 (critical fix):</b> this class now implements
 * {@link AsyncConfigurer}, which {@code pins} the executor used by every
 * plain {@code @Async} method (of which {@code VisitorStatsService} alone has
 * 14). The previous version declared the bean with return type
 * {@code java.util.concurrent.Executor}; Spring's predictive type matching for
 * {@code @Bean} factory methods uses the declared return type, and
 * {@code Executor} is not a {@code TaskExecutor}, so the default-@Async
 * resolution found no usable TaskExecutor and silently fell back to
 * {@code SimpleAsyncTaskExecutor} — which spawns a brand-new OS thread per
 * task and never pools. At human traffic this was invisible; under the k6
 * capacity run it grew unbounded until {@code pthread_create} returned EAGAIN
 * and the JVM started dying with {@code OutOfMemoryError: unable to create
 * native thread} mid-request (Tomcat's own worker add-path failed with it).
 *
 * <p>
 * With {@link AsyncConfigurer} the pooled executor below is the ONLY executor
 * the async machinery can reach — no lookup, no fallback, no surprise. Pool
 * bounds are environment-tunable (see application.properties
 * {@code fmf.async.*}, set from the compose stack) so the same image can be
 * right-sized per host without a rebuild.
 */
@Configuration
@EnableAsync
public class AsyncConfig implements AsyncConfigurer {

        private static final org.slf4j.Logger log =
                        org.slf4j.LoggerFactory.getLogger(AsyncConfig.class);

        @Value("${fmf.async.core-size:2}")
        private int coreSize;

        @Value("${fmf.async.max-size:8}")
        private int maxSize;

        @Value("${fmf.async.queue-capacity:200}")
        private int queueCapacity;

    /**
     * The bounded background pool shared by every plain {@code @Async} method.
     * Visitor-stat updates are best-effort: when the pool AND queue are full
     * the task is logged and dropped instead of failing the viewer's request —
     * the queue rejection never propagates (Phase 0.20 behaviour kept).
     *
     * @return the (single, shared) {@link ThreadPoolTaskExecutor}
     */
    @Bean(name = "applicationTaskExecutor")
    public ThreadPoolTaskExecutor applicationTaskExecutor() {
        int core = Math.max(1, coreSize);
        int max = Math.max(core, maxSize);
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(core);
        executor.setMaxPoolSize(max);
        executor.setQueueCapacity(Math.max(1, queueCapacity));
        executor.setThreadNamePrefix("fmf-async-");
        // The DEFAULT rejection policy (Abort) would throw synchronously into
        // the caller's request thread — under burst load a full async queue
        // once turned viewer requests into 500s. Stats are best-effort:
        // log and drop instead of failing the view.
        executor.setRejectedExecutionHandler((task, executor_) ->
                log.warn("Async executor saturated ({}-{} threads, queue {}) — dropped one background task "
                                + "(e.g. a visitor-stat increment). Request NOT affected. "
                                + "Under sustained stress this is a hint to buffer or batch stats.",
                        core, max, queueCapacity));
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(30);
        executor.initialize();
        return executor;
    }

    /**
     * {@code @Async} default-executor hook. Returning the SAME pool the
     * {@code applicationTaskExecutor} bean exposes guarantees the pooled
     * executor is used regardless of how the runtime resolves default
     * candidates — the SimpleAsyncTaskExecutor fallback can never engage.
     */
    @Override
    public Executor getAsyncExecutor() {
        return applicationTaskExecutor();
    }
}
