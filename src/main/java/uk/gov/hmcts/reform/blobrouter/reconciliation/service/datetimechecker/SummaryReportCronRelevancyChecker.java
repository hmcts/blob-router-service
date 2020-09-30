package uk.gov.hmcts.reform.blobrouter.reconciliation.service.datetimechecker;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.ZonedDateTime;

/**
 * Implementation of {@link DateRelevantForCurrentReconciliationChecker}, which checks time relevancy based on defined
 * cron's expression hour portion. It ignores the date portion of the provided DateTime, i.e. the checker is happy
 * with the datetime
 * if provided hour would still cause the cron to fire on the same day.
 */
@Component
public class SummaryReportCronRelevancyChecker implements DateRelevantForCurrentReconciliationChecker {
    private String usedCronExpression;
    private int endHour;
    private boolean initialized;


    @Autowired
    public SummaryReportCronRelevancyChecker(@Value("${scheduling.task.create-reconciliation-summary-report.cron}")
                                             String cronExpression) {
        usedCronExpression = cronExpression;
        initializeEndHourFromCron();
    }

    private void initializeEndHourFromCron() {
        String[] cronElements = usedCronExpression.split(" ");
        String cronHourPart = cronElements[2];

        if ("*".equals(cronHourPart)) {
            return;
        }

        int indexOfHyphen = cronHourPart.indexOf('-');
        if (indexOfHyphen != -1) {
            endHour = Integer.parseInt(cronHourPart.substring(indexOfHyphen + 1));
        } else {
            endHour = Integer.parseInt(cronHourPart);
        }

        initialized = true;
    }

    @Override
    public boolean isTimeRelevant(ZonedDateTime dateTime) {
        if (!initialized) {
            throw new IllegalStateException("Can't determine relevancy from provided cron expression: "
                                                + usedCronExpression);
        }
        return dateTime.getHour() < endHour;
    }
}
