package uk.gov.hmcts.reform.blobrouter.reconciliation.service.datetimechecker;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.time.Clock;
import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static uk.gov.hmcts.reform.blobrouter.reconciliation.service.datetimechecker.StatementRelevancyForAutomatedReportCheckerTest.CheckerTestScenario.scenario;
import static uk.gov.hmcts.reform.blobrouter.util.TimeZones.EUROPE_LONDON_ZONE_ID;

class StatementRelevancyForAutomatedReportCheckerTest {
    private static final ZonedDateTime NOW_UTC = ZonedDateTime.now(Clock.systemUTC());
    private static final ZonedDateTime NOW_LONDON = ZonedDateTime.now(EUROPE_LONDON_ZONE_ID);
    private static final LocalDate TODAY = LocalDate.now();
    private static final LocalDate TOMORROW = TODAY.plusDays(1);
    private static final LocalDate YESTERDAY = TODAY.minusDays(1);
    private static final LocalDate TWO_DAY_AGO = TODAY.minusDays(2);

    @ParameterizedTest(name = "{arguments}")
    @MethodSource("provideCheckerTestScenarios")
    void should_test_checker_under_provided_scenario(CheckerTestScenario scenario) {
        StatementRelevancyForAutomatedReportChecker checker =
            new StatementRelevancyForAutomatedReportChecker(scenario.cron);

        ZonedDateTime timeUnderTest = scenario.dateTimeUnderTest;
        boolean actualResult = checker.isTimeRelevant(timeUnderTest, scenario.reportDate);

        assertEquals(scenario.expectedResult, actualResult, scenario.errorMessage);
    }

    private static Stream<Arguments> provideCheckerTestScenarios() {
        return Stream.of(
            scenario()
                .withCron("0 */30 6 * * *")
                .withDateTimeUnderTest(NOW_UTC.withHour(5).withMinute(25))
                .withReportDate(YESTERDAY)
                .withExpectedResult(true)
                .withErrorMessage("For the yesterday report, and upload happening at 5:25am the next cron run "
                                      + " will happen at 5:30 so the statement is relevant")
                .build(),
            scenario()
                .withCron("0 */30 2-7 * * *")
                .withDateTimeUnderTest(NOW_UTC.withHour(6).withMinute(25))
                .withReportDate(YESTERDAY)
                .withExpectedResult(true)
                .withErrorMessage("For the yesterday report, and upload happening at 6:25am the next cron run "
                                      + " will happen at 6:30 so the statement is relevant")
                .build(),
            scenario()
                .withCron("0 */30 2-7 * * *")
                .withDateTimeUnderTest(NOW_UTC.withHour(7).withMinute(25))
                .withReportDate(YESTERDAY)
                .withExpectedResult(false)
                .withErrorMessage("For the yesterday report, and upload happening at 7:25am (UTC) it's after 7:30 BST "
                                      + "configured cron so is irrelevant")
                .build(),
            scenario()
                .withCron("0 */30 2-6 * * *")
                .withDateTimeUnderTest(NOW_UTC.minusDays(1).withHour(23))
                .withReportDate(YESTERDAY)
                .withExpectedResult(true)
                .withErrorMessage(
                    "For the yesterday report, and upload happening at 11pm previous text the next cron run "
                        + " will happen at 2:00 so the statement is relevant")
                .build(),
            scenario()
                .withCron("0 */30 2-6 * * *")
                .withDateTimeUnderTest(
                    NOW_LONDON.plusYears(1).withMonth(1).withDayOfMonth(1).withHour(6).withMinute(29)
                )
                .withReportDate(LocalDate.of(TODAY.getYear(), 12, 31))
                .withExpectedResult(true)
                .withErrorMessage(
                    "For the last day of year report, and upload happening at 6:29am (BST) next cron run "
                        + " will happen at 6:30 so the statement is relevant")
                .build(),
            scenario()
                .withCron("0 0 2-6 * * *")
                .withDateTimeUnderTest(NOW_LONDON.withMonth(12).withDayOfMonth(31).withHour(23).withMinute(29))
                .withReportDate(LocalDate.of(TODAY.getYear(), 12, 31))
                .withExpectedResult(true)
                .withErrorMessage(
                    "For the last day of year report, and upload happening the same day at 11:29pm (BST) next cron run "
                        + " will happen at 2am so the statement is relevant")
                .build(),
            scenario()
                .withCron("0 */30 2-6 * * *")
                .withDateTimeUnderTest(NOW_UTC.withHour(6).withMinute(25))
                .withReportDate(TWO_DAY_AGO)
                .withExpectedResult(false)
                .withErrorMessage("Statement for the report submitted two days ago is irrelevant")
                .build(),
            scenario()
                .withCron("0 30 3 * * *")
                .withDateTimeUnderTest(NOW_UTC.withHour(2).withMinute(31))
                .withReportDate(YESTERDAY)
                .withExpectedResult(false)
                .withErrorMessage("Statement is irrelevant - miss the cron by one minute "
                                      + "(CRON is BST, provided 2:31 UTC)")
                .build(),
            scenario()
                .withCron("0 30 3 * * *")
                .withDateTimeUnderTest(NOW_LONDON.withHour(3).withMinute(29).withSecond(59))
                .withReportDate(YESTERDAY)
                .withExpectedResult(true)
                .withErrorMessage("Statement is relevant - the next cron will run in one minute")
                .build(),
            scenario()
                .withCron("0 */30 2 * * *")
                .withDateTimeUnderTest(NOW_UTC.withHour(1).withMinute(30))
                .withReportDate(YESTERDAY)
                .withExpectedResult(false)
                .withErrorMessage("Statement is irrelevant - we hit exactly the cron time, "
                                      + "the last report is being generated")
                .build(),
            scenario()
                .withCron("0 */30 2 * * *")
                .withDateTimeUnderTest(NOW_LONDON.withHour(2).withMinute(15))
                .withReportDate(TOMORROW)
                .withExpectedResult(true)
                .withErrorMessage("Statement is relevant - it's for tomorrow's report "
                                      + "(which will be generated in 2 days")
                .build(),
            scenario()
                .withCron("0 */30 2 * * *")
                .withDateTimeUnderTest(NOW_LONDON.withHour(1).withMinute(45))
                .withReportDate(TODAY)
                .withExpectedResult(true)
                .withErrorMessage("Statement is relevant - it's for today's report (which will be generated tomorrow")
                .build()
        );

    }

    static class CheckerTestScenario {
        public String cron;
        public boolean expectedResult;
        public ZonedDateTime dateTimeUnderTest;
        public String errorMessage;
        public LocalDate reportDate;

        public CheckerTestScenario withCron(String cron) {
            this.cron = cron;
            return this;
        }

        static CheckerTestScenario scenario() {
            return new CheckerTestScenario();
        }

        public CheckerTestScenario withExpectedResult(boolean expectedResult) {
            this.expectedResult = expectedResult;
            return this;
        }

        public CheckerTestScenario withDateTimeUnderTest(ZonedDateTime dateTimeUnderTest) {
            this.dateTimeUnderTest = dateTimeUnderTest;
            return this;
        }

        public CheckerTestScenario withErrorMessage(String errorMessage) {
            this.errorMessage = errorMessage;
            return this;
        }

        public CheckerTestScenario withReportDate(LocalDate reportDate) {
            this.reportDate = reportDate;
            return this;
        }

        @Override
        public String toString() {
            return
                "cron='" + cron + '\''
                    + ", expectedResult=" + expectedResult
                    + ", dateTimeUnderTest=" + dateTimeUnderTest
                    + ", errorMessage='" + errorMessage + '\''
                    + ", reportDate=" + reportDate;
        }

        public Arguments build() {
            return Arguments.of(this);
        }
    }

}
