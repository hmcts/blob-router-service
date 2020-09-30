package uk.gov.hmcts.reform.blobrouter.reconciliation.service.datetimechecker;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.time.ZonedDateTime;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static uk.gov.hmcts.reform.blobrouter.util.TimeZones.EUROPE_LONDON_ZONE_ID;

class SummaryCronRelevancyCheckerTest {

    @ParameterizedTest(name = "{arguments}")
    @MethodSource("provideCheckerTestScenarios")
    void should_test_checker_under_provided_scenario(CheckerTestScenario scenario) {
        SummaryReportCronRelevancyChecker checker = new SummaryReportCronRelevancyChecker(scenario.cron);

        ZonedDateTime timeUnderTest = currentTimeWithHourShiftedTo(scenario.hourUnderTest);
        boolean actualResult = checker.isTimeRelevant(timeUnderTest);

        assertEquals(scenario.expectedResult, actualResult, scenario.errorMessage);
    }

    @Test
    void should_raise_an_exception_when_using_checker_which_cant_be_initialized() {
        SummaryReportCronRelevancyChecker checkerForMinuteCron = new SummaryReportCronRelevancyChecker("* */1 * * * *");

        ZonedDateTime fiveAm = currentTimeWithHourShiftedTo(5);
        Assertions.assertThrows(IllegalStateException.class, () ->
            checkerForMinuteCron.isTimeRelevant(fiveAm)
        );
    }

    private static Stream<Arguments> provideCheckerTestScenarios() {
        return Stream.of(
            Arguments.of(new CheckerTestScenario("0 */30 1-6 * * *", true, 5, "The next cron would fire at 6am so 5am is relevant")),
            Arguments.of(new CheckerTestScenario("0 */30 6 * * *", false, 7, "The next cron would fire next day at 1am so 7am is irrelevant")),
            Arguments.of(new CheckerTestScenario("0 */30 3 * * *", false, 3, "The next cron would fire next day at 1am so 3am is irrelevant")),
            Arguments.of(new CheckerTestScenario("0 */30 11 * * *", true, 10, "The next cron would fire at 11am so 10am is relevant"))
        );
    }

    private ZonedDateTime currentTimeWithHourShiftedTo(int hour) {
        ZonedDateTime now = ZonedDateTime.now(EUROPE_LONDON_ZONE_ID);
        return now.withHour(hour);
    }

    private static class CheckerTestScenario {
        public String cron;
        public boolean expectedResult;
        public int hourUnderTest;
        public String errorMessage;

        public CheckerTestScenario(String cron, boolean expectedResult, int hourUnderTest, String errorMessage) {
            this.cron = cron;
            this.expectedResult = expectedResult;
            this.hourUnderTest = hourUnderTest;
            this.errorMessage = errorMessage;
        }

        @Override
        public String toString() {
            return "cron=" + cron +
                ", expectedResult=" + expectedResult +
                ", hourUnderTest=" + hourUnderTest +
                ", errorMessage='" + errorMessage + '\'';
        }
    }

}
