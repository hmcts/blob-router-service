package uk.gov.hmcts.reform.blobrouter.config;

import com.microsoft.applicationinsights.web.internal.RequestTelemetryContext;
import com.microsoft.applicationinsights.web.internal.ThreadContext;
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
import java.util.function.Supplier;

import static uk.gov.hmcts.reform.blobrouter.util.TimeZones.nowInLondon;

@Configuration
public class SchedulerConfiguration implements SchedulingConfigurer {

    private static final int POOL_SIZE = 10;
    private static AtomicInteger errorCount = new AtomicInteger(0);
    private static final Logger log = LoggerFactory.getLogger(SchedulerConfiguration.class);

    private static final Supplier<Long> CURRENT_MILLIS_SUPPLIER = () -> nowInLondon().toInstant().toEpochMilli();

    private static final Supplier<RequestTelemetryContext> REQUEST_CONTEXT_SUPPLIER = () ->
        new RequestTelemetryContext(CURRENT_MILLIS_SUPPLIER.get(), null);

    @Override
    public void configureTasks(ScheduledTaskRegistrar taskRegistrar) {
        taskRegistrar.setTaskScheduler(routerTaskScheduler());
    }

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
     * Custom {@link ThreadPoolTaskScheduler} to be able to register scheduled tasks via AppInsights.
     */
    private static class RouterTaskScheduler extends ThreadPoolTaskScheduler {

        private static final long serialVersionUID = 8789551087577035719L;

        @Override
        public void execute(Runnable command) {
            super.execute(getWrappedRunnable(command));
        }

        @Override
        public ScheduledFuture<?> schedule(Runnable task, Trigger trigger) {
            return super.schedule(new WrappedRunnable(task, REQUEST_CONTEXT_SUPPLIER.get()), trigger);
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
            return new WrappedRunnable(task, REQUEST_CONTEXT_SUPPLIER.get());
        }
    }

    private static class WrappedRunnable implements Runnable {

        private final Runnable task;
        private RequestTelemetryContext requestContext;

        WrappedRunnable(Runnable task, RequestTelemetryContext requestContext) {
            this.task = task;
            this.requestContext = requestContext;
        }

        @Override
        public void run() {
            if (ThreadContext.getRequestTelemetryContext() != null) {
                ThreadContext.remove();

                // since this runnable is ran on schedule, update the context on every run
                requestContext = REQUEST_CONTEXT_SUPPLIER.get();
            }

            ThreadContext.setRequestTelemetryContext(requestContext);

            task.run();
        }
    }
}
