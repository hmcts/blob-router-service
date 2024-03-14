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

    /**
     * The function `isTimeRelevant` determines if a statement uploaded at a specific date
     * and time is within the deadline for processing based on a given report day.
     *
     * @param statetementUploadDateTime The `statetementUploadDateTime` parameter represents the date and time when a
     *                                  statement was uploaded. This method is checking if the uploaded statement
     *                                  is relevant based on the `reportDay` provided.
     * @param reportDay                 The `reportDay` parameter represents the day for which the report is being
     *                                  generated. In the provided code snippet, it is used to calculate the day for
     *                                  which the cron job is triggered (`cronRunForSpecifiedReport`) by adding
     *                                  one day to the `reportDay`.
     * @return The method `isTimeRelevant` returns a boolean value indicating whether the statement upload date and the
     * next cron run date are within a certain number of days of each other.
     */
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

    /**
     * The function `isWithinDays` checks if the difference in milliseconds between two dates is within a
     * specified number of days.
     *
     * @param statementUploadTime        The `statementUploadTime` parameter represents the time when a statement was
     *                                   uploaded. It is of type `Date`.
     * @param nextCronRunAfterUploadTime nextCronRunAfterUploadTime is the timestamp of the next scheduled cron
     *                                   job after the statement upload time.
     * @param daysDifference             The `daysDifference` parameter represents the number of days within which
     *                                   you want to check if the `statementUploadTime` is within,
     *                                   relative to the `nextCronRunAfterUploadTime`.
     * @return The method is returning a boolean value indicating whether the difference in milliseconds between the
     * scheduled cron run time and the statement upload time is less than the specified
     * number of milliseconds within the requested number of days.
     */
    private boolean isWithinDays(Date statementUploadTime, Date nextCronRunAfterUploadTime, long daysDifference) {
        long differenceInMilisecondsBetweenScheduledCronAndUploadTime =
            nextCronRunAfterUploadTime.getTime() - statementUploadTime.getTime();
        long milisecondsWithinRequestsDays = MILISECONDS_IN_24_HOURS * (daysDifference + 1);
        return differenceInMilisecondsBetweenScheduledCronAndUploadTime < milisecondsWithinRequestsDays;
    }

    /**
     * The function prepares a cron expression by updating the day and month values based on a specified report day.
     *
     * @param cronRunForSpecifiedReport The method `prepareCronExpressionForSpecifiedReportDay` takes a `LocalDate`
     *                                  parameter named `cronRunForSpecifiedReport`, which represents the date for
     *                                  which the cron expression
     *                                  needs to be prepared.
     * @return The method `prepareCronExpressionForSpecifiedReportDay` returns a modified cron expression where the
     * day and month fields are updated based on the `cronRunForSpecifiedReport` parameter.
     */
    private String prepareCronExpressionForSpecifiedReportDay(LocalDate cronRunForSpecifiedReport) {
        String[] cronParts = cron.split(" "); //0 - second 1 - minute 2 - hour 3 - day 4 - month
        cronParts[3] = "" + cronRunForSpecifiedReport.getDayOfMonth();
        cronParts[4] = "" + cronRunForSpecifiedReport.getMonthValue();

        return String.join(" ", cronParts);
    }
}
