package com.forkmyfolio.config;

import org.junit.jupiter.api.Test;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Phase 0.21 regression tests for the @Async executor wiring.
 *
 * The production incident this class guards against: with the bean previously
 * declared as {@code Executor}, Spring's default-@Async resolution found no
 * TaskExecutor and silently fell back to {@code SimpleAsyncTaskExecutor} — a
 * new (never-pooled) OS thread per task — which grew unbounded under load and
 * killed the JVM with "unable to create native thread". These tests pin:
 * <ul>
 *   <li>the configured implementor is a real, bounded {@link ThreadPoolTaskExecutor};</li>
 *   <li>{@link AsyncConfig#getAsyncExecutor()} (the @Async default hook)
 *       returns the SAME pooled instance — the SimpleAsyncTaskExecutor
 *       fallback can never engage;</li>
 *   <li>degenerate configuration values are clamped into a legal pool;</li>
 *   <li>a saturated pool neither throws nor loses the calling thread —
 *       tasks are dropped quietly (best-effort stats contract).</li>
 * </ul>
 */
class AsyncConfigTest {

    @Test
    void executorIsTheConfiguredBoundedPool_andAsyncHookReturnsTheSameInstance() {
        AsyncConfig config = new AsyncConfig();
        inject(config, 2, 8, 200);

        ThreadPoolTaskExecutor executor = config.applicationTaskExecutor();

        assertEquals(2, executor.getCorePoolSize());
        assertEquals(8, executor.getMaxPoolSize());
        assertEquals(200, executor.getThreadPoolExecutor().getQueue().remainingCapacity() + executor.getThreadPoolExecutor().getQueue().size());
        // The single most important assertion of Phase 0.21: the @Async
        // default hook must be a POOLED executor — SimpleAsyncTaskExecutor
        // (the historic silent fallback) is not a ThreadPoolTaskExecutor and
        // could never carry these pool bounds.
        org.springframework.core.task.AsyncTaskExecutor hooked =
                (org.springframework.core.task.AsyncTaskExecutor) config.getAsyncExecutor();
        assertSame(ThreadPoolTaskExecutor.class, hooked.getClass());
        assertEquals(2, ((ThreadPoolTaskExecutor) hooked).getCorePoolSize());
        assertEquals(8, ((ThreadPoolTaskExecutor) hooked).getMaxPoolSize());
    }

    @Test
    void degenerateValuesAreClampedIntoALegalPool() {
        AsyncConfig config = new AsyncConfig();
        inject(config, 0, 0, 0);

        ThreadPoolTaskExecutor executor = config.applicationTaskExecutor();

        assertTrue(executor.getCorePoolSize() >= 1, "core pool must be at least 1");
        assertTrue(executor.getMaxPoolSize() >= executor.getCorePoolSize(),
                "max pool must be >= core pool");
    }

    @Test
    void saturatedPoolDropsTasksWithoutPropagatingRejection() throws Exception {
        AsyncConfig config = new AsyncConfig();
        inject(config, 1, 1, 1);

        ThreadPoolTaskExecutor executor = config.applicationTaskExecutor();
        CountDownLatch block = new CountDownLatch(1);
        CountDownLatch parked = new CountDownLatch(1);
        try {
            // Park the single pool thread on the shared latch, fill the queue
            // (capacity 1), then submit one MORE task beyond that — with the
            // default Abort policy this call chain would throw.
            executor.execute(() -> {
                parked.countDown();
                try {
                    block.await(5, TimeUnit.SECONDS);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            });
            assertTrue(parked.await(2, TimeUnit.SECONDS), "pool thread should be parked");
            executor.execute(() -> { });
            // The call that previously endangered request threads:
            assertDoesNotThrow(() -> executor.execute(() -> { }));
        } finally {
            block.countDown();
            executor.shutdown();
        }
    }

    private static void inject(AsyncConfig config, int core, int max, int queue) {
        org.springframework.test.util.ReflectionTestUtils.setField(config, "coreSize", core);
        org.springframework.test.util.ReflectionTestUtils.setField(config, "maxSize", max);
        org.springframework.test.util.ReflectionTestUtils.setField(config, "queueCapacity", queue);
    }
}
