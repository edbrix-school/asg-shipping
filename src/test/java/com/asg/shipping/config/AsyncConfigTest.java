package com.asg.shipping.config;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.ThreadPoolExecutor;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AsyncConfigTest {

    private final ThreadPoolTaskExecutor executor = new AsyncConfig().lovLookupExecutor();

    @AfterEach
    void tearDown() {
        executor.shutdown();
    }

    /**
     * A ThreadPoolExecutor only adds threads once its queue is full, so a max above the core size
     * would never take effect behind a deep queue. Keeping them equal is what makes the fan-out
     * actually run in parallel.
     */
    @Test
    void coreAndMaxAreEqualSoTheFanOutIsReallyParallel() {
        assertEquals(executor.getCorePoolSize(), executor.getMaxPoolSize());
    }

    @Test
    void poolStaysWithinTheHikariConnectionLimit() {
        assertTrue(executor.getMaxPoolSize() < 20,
                "each task can hold a DB connection; the pool must stay under Hikari's maximum-pool-size");
    }

    @Test
    void queueIsBoundedAndOverflowRunsOnTheCaller() {
        executor.initialize();
        ThreadPoolExecutor pool = executor.getThreadPoolExecutor();

        assertEquals(0, pool.getQueue().size());
        assertTrue(pool.getQueue().remainingCapacity() < Integer.MAX_VALUE, "queue must be bounded");
        assertInstanceOf(ThreadPoolExecutor.CallerRunsPolicy.class, pool.getRejectedExecutionHandler());
    }

    @Test
    void tasksRunAndCarryTheDedicatedThreadName() throws Exception {
        executor.initialize();

        String threadName = executor.submit(() -> Thread.currentThread().getName()).get();

        assertTrue(threadName.startsWith("lov-lookup-"), "unexpected thread name: " + threadName);
    }
}
