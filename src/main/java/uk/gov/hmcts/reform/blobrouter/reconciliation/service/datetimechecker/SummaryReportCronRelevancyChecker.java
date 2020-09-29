package uk.gov.hmcts.reform.blobrouter.reconciliation.service.datetimechecker;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.ZonedDateTime;

/**
 * Implementation of {@link DateRelevantForCurrentReconciliationChecker}, which checks time relevancy based on defined
 * cron's expression hour portion. It ignores the date portion of the provided DateTime, i.e. the checker is happy with the datetime
 * if provided hour would still cause the cron to fire on the same day.
 */
@Component
public class SummaryReportCronRelevancyChecker implements DateRelevantForCurrentReconciliationChecker {
    private final int endHour;

    @Autowired
    public SummaryReportCronRelevancyChecker(@Value("${scheduling.task.create-reconciliation-summary-report.cron}") String cronExpression) {
        endHour = extractEndHourFromCron(cronExpression);
    }

    private int extractEndHourFromCron(String cronExpression) {
        String[] cronElements = cronExpression.split(" ");
        String cronHourPart = cronElements[2];

        int indexOfHyphen = cronHourPart.indexOf('-');
        if (indexOfHyphen != -1) {
            return Integer.parseInt(cronHourPart.substring(indexOfHyphen + 1));
        } else {
            return Integer.parseInt(cronHourPart);
        }
    }

    @Override
    public boolean isTimeRelevant(ZonedDateTime dateTime) {
        return dateTime.getHour() < endHour;
    }
}
