package uk.gov.hmcts.reform.blobrouter.reconciliation.service.datetimechecker;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.support.CronSequenceGenerator;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.util.Date;

import static java.util.TimeZone.getTimeZone;
import static uk.gov.hmcts.reform.blobrouter.util.TimeZones.EUROPE_LONDON_ZONE_ID;

/**
 * Implementation of {@link DateRelevantForCurrentReconciliationChecker}, which checks time relevancy based on defined
 * cron's expression.
 */
@Component
public class SummaryReportCronAwareChecker implements DateRelevantForCurrentReconciliationChecker {
    private final String cron;

    public SummaryReportCronAwareChecker(
        @Value("${scheduling.task.create-reconciliation-summary-report.cron}")
        String cron) {
        this.cron = cron;
    }

    @Override
    public boolean isTimeRelevant(ZonedDateTime dateTime, LocalDate reportDay) {
        //reports are always run for day before so, cron triggered today should process yesterday entries
        LocalDate cronRunForSpecifiedReport = reportDay.plusDays(1);

        String[] cronParts = cron.split(" "); //0 - second 1 - minute 2 - hour 3 - day 4 - month
        cronParts[3] = "" + cronRunForSpecifiedReport.getDayOfMonth();
        cronParts[4] = "" + cronRunForSpecifiedReport.getMonthValue();

        String cronExpressionForGivenReport = String.join(" ", cronParts);

        CronSequenceGenerator cronSequenceGenerator = new CronSequenceGenerator(cronExpressionForGivenReport,
                                                                                getTimeZone(EUROPE_LONDON_ZONE_ID));

        Date requestedDate = Date.from(dateTime.toInstant());
        Date next = cronSequenceGenerator.next(requestedDate);

        return next.getTime() - requestedDate.getTime() < 1000 * 60 * 60 * 24;
    }
}
