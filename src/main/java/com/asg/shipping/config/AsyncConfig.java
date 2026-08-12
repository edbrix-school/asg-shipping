package com.asg.shipping.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.ThreadPoolExecutor;

/**
 * Dedicated pool for the parallel detail-table and LOV lookups in the document-fetch services
 * ({@code ImportManifestServiceImpl}, {@code ManifestCorrectorServiceImpl}).
 *
 * Those services inject {@code @Qualifier("lovLookupExecutor") Executor}, but no such bean existed:
 * there is no {@code lombok.config}, so Lombok never copied the qualifier onto the generated
 * constructor parameter and the injection silently resolved by type to Spring Boot's
 * auto-configured {@code applicationTaskExecutor} — 8 threads shared with the rest of the
 * application, and an unbounded queue that stops the pool ever growing past those 8.
 *
 * Declaring an {@code Executor} bean here switches off that auto-configuration
 * ({@code TaskExecutionAutoConfiguration} backs off on any {@code Executor} bean). Nothing else in
 * the service or in asg-common-lib used it — there is no {@code @EnableAsync}, no {@code @Async},
 * and no MVC async return type anywhere. If {@code @Async} is ever introduced, give it its own
 * executor rather than letting it share this one.
 */
@Configuration
public class AsyncConfig {

    /**
     * Sized against the Hikari pool ({@code maximum-pool-size=20}): each task can hold a connection
     * for its query, so this stays well under that limit to leave headroom for the request threads
     * themselves and for every other endpoint.
     */
    private static final int POOL_SIZE = 12;

    private static final int QUEUE_CAPACITY = 200;

    @Bean
    public ThreadPoolTaskExecutor lovLookupExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        // Core and max are equal on purpose: a ThreadPoolExecutor only grows past the core size once
        // the queue is full, so a larger max with a deep queue would never actually add threads.
        executor.setCorePoolSize(POOL_SIZE);
        executor.setMaxPoolSize(POOL_SIZE);
        executor.setQueueCapacity(QUEUE_CAPACITY);
        executor.setThreadNamePrefix("lov-lookup-");
        // Bounded queue plus caller-runs degrades to sequential execution on the request thread
        // under load instead of rejecting the lookup or queueing without limit.
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        executor.setAllowCoreThreadTimeOut(true);
        executor.setKeepAliveSeconds(60);
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(30);
        return executor;
    }
}
