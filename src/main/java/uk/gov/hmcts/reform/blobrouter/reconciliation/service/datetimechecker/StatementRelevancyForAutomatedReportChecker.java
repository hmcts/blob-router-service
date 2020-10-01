package uk.gov.hmcts.reform.blobrouter.reconciliation.service.datetimechecker;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.support.CronSequenceGenerator;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Date;

import static java.util.TimeZone.getTimeZone;
import static uk.gov.hmcts.reform.blobrouter.util.TimeZones.EUROPE_LONDON_ZONE_ID;

/**
 * Class for checking if the provided date for given report date is relevant for automated report generation.
 * The date is relevant only if it's before another automated trigger of the reporting cron.
 */
@Component
public class StatementRelevancyForAutomatedReportChecker {
    private static final int MILISECONDS_IN_SECOND = 1000;
    private static final int SECONDS_IN_MINUTE = 60;
    private static final int MINUTES_IN_HOUR = 60;
    private static final int HOURS_IN_DAY = 24;
    private static final int MILISECONDS_IN_24_HOURS =
        MILISECONDS_IN_SECOND * SECONDS_IN_MINUTE * MINUTES_IN_HOUR * HOURS_IN_DAY;
    private final String cron;

    public StatementRelevancyForAutomatedReportChecker(
        @Value("${scheduling.task.create-reconciliation-summary-report.cron}")
        String cron
    ) {
        this.cron = cron;
    }

    public boolean isTimeRelevant(ZonedDateTime statetementUploadDateTime, LocalDate reportDay) {
        //reports are always run for day before so, cron triggered today should process yesterday envelopes
        //we want to ensure if Cron narrowed down to requested reportDay + 1, is going to be run in 24 hrs
        //if not, it means the deadline for the statement passed

        LocalDate cronRunForSpecifiedReport = reportDay.plusDays(1);
        String cronExpressionForGivenReport = prepareCronExpressionForSpecifiedReportDay(cronRunForSpecifiedReport);

        var cronSequenceGenerator = new CronSequenceGenerator(
            cronExpressionForGivenReport,
            getTimeZone(EUROPE_LONDON_ZONE_ID)
        );

        Date statementUploadInstant = Date.from(statetementUploadDateTime.toInstant());
        Date nextCronRunAfterUploadTime = cronSequenceGenerator.next(statementUploadInstant);

        //The shift below is possible if the checking should work for statements uploaded in the future
        //See the unit tests for TODAY and TOMORROW reports, this shift is required to support those scenario
        //If that functionality is not required this can be removed, then the answer from this method will be unexpected
        long daysBetweenWhenReportIsRunAndUploadedDate =
            ChronoUnit.DAYS.between(cronRunForSpecifiedReport, statetementUploadDateTime);
        return isWithinDays(statementUploadInstant, nextCronRunAfterUploadTime,
                            Math.abs(daysBetweenWhenReportIsRunAndUploadedDate)
        );
    }

    private boolean isWithinDays(Date statementUploadTime, Date nextCronRunAfterUploadTime, long daysDifference) {
        long differenceInMilisecondsBetweenScheduledCronAndUploadTime =
            nextCronRunAfterUploadTime.getTime() - statementUploadTime.getTime();
        long milisecondsWithinRequestsDays = MILISECONDS_IN_24_HOURS * (daysDifference + 1);
        return differenceInMilisecondsBetweenScheduledCronAndUploadTime < milisecondsWithinRequestsDays;
    }

    private String prepareCronExpressionForSpecifiedReportDay(LocalDate cronRunForSpecifiedReport) {
        String[] cronParts = cron.split(" "); //0 - second 1 - minute 2 - hour 3 - day 4 - month
        cronParts[3] = "" + cronRunForSpecifiedReport.getDayOfMonth();
        cronParts[4] = "" + cronRunForSpecifiedReport.getMonthValue();

        return String.join(" ", cronParts);
    }
}
