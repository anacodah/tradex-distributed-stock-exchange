package com.tradex.node.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

@Configuration
public class ThreadPoolConfig {

    /**
     * Bounded thread pool for processing incoming client/remote orders.
     */
    @Bean(name = "orderProcessingExecutor")
    public ThreadPoolExecutor orderProcessingExecutor() {
        return new ThreadPoolExecutor(
                4,                                  // Core pool size
                16,                                 // Max pool size
                60L, TimeUnit.SECONDS,              // Keep-alive time
                new ArrayBlockingQueue<>(500),      // Bounded queue
                new NamedThreadFactory("order-worker-"),
                new ThreadPoolExecutor.CallerRunsPolicy() // Bounded backpressure protection
        );
    }

    /**
     * Bounded thread pool for inter-node replication and consensus messages.
     */
    @Bean(name = "replicationExecutor")
    public ThreadPoolExecutor replicationExecutor() {
        return new ThreadPoolExecutor(
                2,
                8,
                60L, TimeUnit.SECONDS,
                new ArrayBlockingQueue<>(1000),
                new NamedThreadFactory("repl-worker-"),
                new ThreadPoolExecutor.DiscardOldestPolicy()
        );
    }

    /**
     * Bounded scheduled executor for heartbeats, election timeouts, and clock sync.
     */
    @Bean(name = "clusterScheduledExecutor")
    public ScheduledExecutorService clusterScheduledExecutor() {
        return Executors.newScheduledThreadPool(4, new NamedThreadFactory("cluster-sched-"));
    }

    private static class NamedThreadFactory implements ThreadFactory {
        private final String prefix;
        private final AtomicInteger threadNumber = new AtomicInteger(1);

        public NamedThreadFactory(String prefix) {
            this.prefix = prefix;
        }

        @Override
        public Thread newThread(Runnable r) {
            Thread t = new Thread(r, prefix + threadNumber.getAndIncrement());
            t.setDaemon(true);
            return t;
        }
    }
}
