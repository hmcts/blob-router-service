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
    private static final ZonedDateTime UTC_TIME_DURING_BST = ZonedDateTime.now(Clock.systemUTC())
        .withYear(2020).withMonth(10).withDayOfMonth(7);
    private static final ZonedDateTime UTC_TIME_DURING_GMT = ZonedDateTime.now(Clock.systemUTC())
        .withYear(2020).withMonth(10).withDayOfMonth(30);

    private static final ZonedDateTime NOW_LONDON = ZonedDateTime.now(EUROPE_LONDON_ZONE_ID);
    private static final ZonedDateTime LONDON_GMT = ZonedDateTime.now(EUROPE_LONDON_ZONE_ID);

    private static final LocalDate TODAY = LocalDate.now();
    private static final LocalDate TOMORROW = TODAY.plusDays(1);
    private static final LocalDate YESTERDAY = TODAY.minusDays(1);

    @ParameterizedTest(name = "{arguments}")
    @MethodSource("provideScenarios")
    void should_test_checker_under_provided_scenario(CheckerTestScenario scenario) {
        StatementRelevancyForAutomatedReportChecker checker =
            new StatementRelevancyForAutomatedReportChecker(scenario.cron);

        ZonedDateTime timeUnderTest = scenario.dateTimeUnderTest;
        boolean actualResult = checker.isTimeRelevant(timeUnderTest, scenario.reportDate);

        assertEquals(scenario.expectedResult, actualResult, scenario.errorMessage);
    }

    @ParameterizedTest(name = "{arguments}")
    @MethodSource("provideTimezoneScenarios")
    void checker_should_deal_with_timezones(CheckerTestScenario scenario) {
        StatementRelevancyForAutomatedReportChecker checker =
            new StatementRelevancyForAutomatedReportChecker(scenario.cron);

        ZonedDateTime timeUnderTest = scenario.dateTimeUnderTest;
        boolean actualResult = checker.isTimeRelevant(timeUnderTest, scenario.reportDate);

        assertEquals(scenario.expectedResult, actualResult, scenario.errorMessage);
    }

    private static Stream<Arguments> provideScenarios() {
        return Stream.of(
            scenario()
                .withCron("0 */30 2-6 * * *")
                .withDateTimeUnderTest(NOW_LONDON.withHour(4))
                .withReportDate(YESTERDAY)
                .withExpectedResult(true)
                .withErrorMessage("Statement is relevant - the next cron will run at 4:30am")
                .build(),
            scenario()
                .withCron("0 0 2-6 * * *")
                .withDateTimeUnderTest(NOW_LONDON.withMonth(12).withDayOfMonth(31).withHour(23).withMinute(29))
                .withReportDate(LocalDate.of(TODAY.getYear(), 12, 31))
                .withExpectedResult(true)
                .withErrorMessage(
                    "For the last day of year report, and upload happening the same day at 11:29pm next cron run "
                        + " will happen at 2am so the statement is relevant")
                .build(),
            scenario()
                .withCron("0 30 3 * * *")
                .withDateTimeUnderTest(NOW_LONDON.withHour(3).withMinute(29).withSecond(59))
                .withReportDate(YESTERDAY)
                .withExpectedResult(true)
                .withErrorMessage("Statement is relevant - the next cron will run in one second")
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

    private static Stream<Arguments> provideTimezoneScenarios() {
        return Stream.of(
            scenario()
                .withCron("0 */30 6 * * *")
                .withDateTimeUnderTest(UTC_TIME_DURING_BST.withHour(5).withMinute(25))
                .forYesterdaysReport()
                .withExpectedResult(true)
                .withErrorMessage("For the yesterday report, and upload happening at (5:25 UTC) 6:25am BST "
                                      + "the next cron run will happen at 6:30 BST so the statement is relevant")
                .build(),
            scenario()
                .withCron("0 30 6 * * *")
                .withDateTimeUnderTest(LONDON_GMT.withHour(5).withMinute(25))
                .forYesterdaysReport()
                .withExpectedResult(true)
                .withErrorMessage("For the yesterday report, and upload happening at (5:25am GMT) "
                                      + "the next cron run will happen at 5:30 GMT so the statement is relevant")
                .build(),
            scenario()
                .withCron("0 */30 2-5 * * *")
                .withDateTimeUnderTest(UTC_TIME_DURING_GMT.withHour(5).withMinute(25))
                .forYesterdaysReport()
                .withExpectedResult(true)
                .withErrorMessage("For the yesterday report, and upload happening at 5:25am UTC the next cron run "
                                      + " will happen at 5:30 GMT so the statement is relevant")
                .build(),
            scenario()
                .withCron("0 */30 2-7 * * *")
                .withDateTimeUnderTest(UTC_TIME_DURING_BST.withHour(7).withMinute(25))
                .forYesterdaysReport()
                .withExpectedResult(false)
                .withErrorMessage("For the yesterday report, and upload happening at 7:25am (UTC) it's after 7:30 BST "
                                      + "configured cron so statement is irrelevant")
                .build(),
            scenario()
                .withCron("0 */30 2-6 * * *")
                .withDateTimeUnderTest(UTC_TIME_DURING_BST.minusDays(1).withHour(23))
                .withReportDate(UTC_TIME_DURING_BST.toLocalDate())
                .withExpectedResult(true)
                .withErrorMessage(
                    "For the yesterday report, and upload happening at 11pm previous text the next cron run "
                        + " will happen at 2:00 so the statement is relevant")
                .build(),
            scenario()
                .withCron("0 */30 2-6 * * *")
                .withDateTimeUnderTest(UTC_TIME_DURING_GMT.withHour(6).withMinute(25))
                .forOutdatedReport()
                .withExpectedResult(false)
                .withErrorMessage("Statement for the report submitted couple days ago is irrelevant")
                .build(),
            scenario()
                .withCron("0 30 3 * * *")
                .withDateTimeUnderTest(UTC_TIME_DURING_BST.withHour(2).withMinute(31))
                .forYesterdaysReport()
                .withExpectedResult(false)
                .withErrorMessage("Statement is irrelevant - miss the cron by one minute "
                                      + "(CRON is BST, provided 2:31 UTC)")
                .build(),
            scenario()
                .withCron("0 30 3 * * *")
                .withDateTimeUnderTest(UTC_TIME_DURING_GMT.withHour(2).withMinute(31))
                .forYesterdaysReport()
                .withExpectedResult(true)
                .withErrorMessage("Statement is relevant - 59 minutes to spare "
                                      + "(CRON is GMT 3:30, provided 2:31 UTC)")
                .build(),
            scenario()
                .withCron("0 */30 2 * * *")
                .withDateTimeUnderTest(UTC_TIME_DURING_BST.withHour(1).withMinute(30))
                .forYesterdaysReport()
                .withExpectedResult(false)
                .withErrorMessage("Statement is irrelevant - we hit exactly the cron time, "
                                      + "the last report is being generated")
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

        public CheckerTestScenario forYesterdaysReport() {
            this.reportDate = dateTimeUnderTest.toLocalDate().minusDays(1);
            return this;
        }

        public CheckerTestScenario forOutdatedReport() {
            this.reportDate = dateTimeUnderTest.toLocalDate().minusDays(2);
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
