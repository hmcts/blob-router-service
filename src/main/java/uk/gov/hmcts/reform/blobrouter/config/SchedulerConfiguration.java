package uk.gov.hmcts.reform.blobrouter.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.Trigger;
import org.springframework.scheduling.annotation.SchedulingConfigurer;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.scheduling.config.ScheduledTaskRegistrar;

import java.util.Date;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * The `SchedulerConfiguration` class in Java configures a custom `ThreadPoolTaskScheduler` for routing tasks with error
 * handling and consistent task wrapping.
 */
@Configuration
public class SchedulerConfiguration implements SchedulingConfigurer {

    private static final int POOL_SIZE = 10;
    private static final AtomicInteger errorCount = new AtomicInteger(0);
    private static final Logger log = LoggerFactory.getLogger(SchedulerConfiguration.class);

    @Override
    public void configureTasks(ScheduledTaskRegistrar taskRegistrar) {
        taskRegistrar.setTaskScheduler(routerTaskScheduler());
    }

    /**
     * The function creates a TaskScheduler bean with a ThreadPoolTaskScheduler implementation for routing tasks.
     *
     * @return An instance of `ThreadPoolTaskScheduler` named `scheduler` is being returned.
     */
    @Bean
    public TaskScheduler routerTaskScheduler() {
        ThreadPoolTaskScheduler scheduler = new RouterTaskScheduler();
        scheduler.setPoolSize(POOL_SIZE);
        scheduler.setThreadNamePrefix("ROUTER-");
        scheduler.setErrorHandler(t -> {
            log.error("Unhandled exception during task. {}: {}", t.getClass(), t.getMessage(), t);
            errorCount.incrementAndGet();
        });
        scheduler.initialize();

        return scheduler;
    }

    /**
     * Custom {@link ThreadPoolTaskScheduler} that wraps every task consistently.
     */
    private static class RouterTaskScheduler extends ThreadPoolTaskScheduler {

        private static final long serialVersionUID = 8789551087577035719L;

        @Override
        public void execute(Runnable command) {
            super.execute(getWrappedRunnable(command));
        }

        @Override
        public ScheduledFuture<?> schedule(Runnable task, Trigger trigger) {
            return super.schedule(new WrappedRunnable(task), trigger);
        }

        @Override
        public ScheduledFuture<?> schedule(Runnable task, Date startTime) {
            return super.schedule(getWrappedRunnable(task), startTime);
        }

        @Override
        public ScheduledFuture<?> scheduleAtFixedRate(Runnable task, Date startTime, long period) {
            return super.scheduleAtFixedRate(getWrappedRunnable(task), startTime, period);
        }

        @Override
        public ScheduledFuture<?> scheduleAtFixedRate(Runnable task, long period) {
            return super.scheduleAtFixedRate(getWrappedRunnable(task), period);
        }

        @Override
        public ScheduledFuture<?> scheduleWithFixedDelay(Runnable task, Date startTime, long delay) {
            return super.scheduleWithFixedDelay(getWrappedRunnable(task), startTime, delay);
        }

        @Override
        public ScheduledFuture<?> scheduleWithFixedDelay(Runnable task, long delay) {
            return super.scheduleWithFixedDelay(getWrappedRunnable(task), delay);
        }

        private Runnable getWrappedRunnable(Runnable task) {
            return new WrappedRunnable(task);
        }
    }

    /**
     * The `WrappedRunnable` class implements the `Runnable` interface and wraps another `Runnable` task while
     * preserving a dedicated hook point for scheduled-task execution behavior.
     */
    private static class WrappedRunnable implements Runnable {

        private final Runnable task;

        WrappedRunnable(Runnable task) {
            this.task = task;
        }

        @Override
        public void run() {

            task.run();
        }
    }
}
