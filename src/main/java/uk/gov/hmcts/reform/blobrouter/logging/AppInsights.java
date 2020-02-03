package uk.gov.hmcts.reform.blobrouter.logging;

import com.microsoft.applicationinsights.TelemetryClient;
import com.microsoft.applicationinsights.telemetry.Duration;
import com.microsoft.applicationinsights.telemetry.RequestTelemetry;
import com.microsoft.applicationinsights.web.internal.RequestTelemetryContext;
import com.microsoft.applicationinsights.web.internal.ThreadContext;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.slf4j.Logger;
import org.springframework.stereotype.Component;

import static org.slf4j.LoggerFactory.getLogger;
import static uk.gov.hmcts.reform.blobrouter.util.TimeZones.nowInLondon;

@Aspect
@Component
public class AppInsights {

    private static final Logger log = getLogger(AppInsights.class);

    private final TelemetryClient telemetryClient;

    public AppInsights(TelemetryClient telemetryClient) {
        this.telemetryClient = telemetryClient;
    }

    // schedules

    @Around("@annotation(org.springframework.scheduling.annotation.Scheduled)")
    public void aroundSchedule(ProceedingJoinPoint joinPoint) throws Throwable {
        RequestTelemetryContext requestTelemetry = ThreadContext.getRequestTelemetryContext();
        boolean success = false;

        try {
            joinPoint.proceed();

            success = true;
        } finally {
            handleRequestTelemetry(requestTelemetry, joinPoint.getTarget().getClass().getSimpleName(), success);
        }
    }

    private void handleRequestTelemetry(
        RequestTelemetryContext requestTelemetryContext,
        String caller,
        boolean success
    ) {
        String requestName = "Schedule /" + caller;

        if (requestTelemetryContext != null) {
            handleRequestTelemetry(
                requestTelemetryContext.getHttpRequestTelemetry(),
                requestName,
                requestTelemetryContext.getRequestStartTimeTicks(),
                success
            );
        } else {
            log.warn(
                "Request Telemetry Context has been removed by ThreadContext - cannot log '{}' request",
                requestName
            );
        }
    }

    private void handleRequestTelemetry(
        RequestTelemetry requestTelemetry,
        String requestName,
        long start,
        boolean success
    ) {
        if (requestTelemetry != null) {
            requestTelemetry.setName(requestName);
            requestTelemetry.setDuration(new Duration(
                nowInLondon().toInstant().toEpochMilli() - start
            ));
            requestTelemetry.setSuccess(success);

            telemetryClient.trackRequest(requestTelemetry);
        }
    }
}
