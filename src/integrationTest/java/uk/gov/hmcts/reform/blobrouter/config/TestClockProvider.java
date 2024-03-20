package uk.gov.hmcts.reform.blobrouter.config;

import jakarta.validation.ClockProvider;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import uk.gov.hmcts.reform.blobrouter.util.TimeZones;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;

/**
 * The `TestClockProvider` class provides a custom `ClockProvider` bean with a stopped clock set to the
 * Europe/London time zone.
 */
@TestConfiguration
public class TestClockProvider {

    public static Instant stoppedInstant = Instant.now();

    /**
     * The function stoppedClock() returns a ClockProvider that provides a clock set to the Europe/London time zone.
     *
     * @return A `ClockProvider` bean is being returned, which provides a stopped clock set to the time zone
     *      `Europe/London`.
     */
    @Bean
    @Primary
    public ClockProvider stoppedClock() {
        return () -> provideClock(TimeZones.EUROPE_LONDON_ZONE_ID);
    }

    /**
     * The `provideClock` function creates a custom Clock instance based on a specified ZoneId.
     *
     * @param zoneId ZoneId is a class in Java that represents a time zone identifier. It is used to identify the
     *      time zone for which the clock is being created in the `provideClock` method.
     * @return A custom implementation of the Clock interface is being returned. This implementation uses the provided
     *      zoneId for time zone information and handles the instant based on whether a stoppedInstant is set or not.
     */
    private Clock provideClock(ZoneId zoneId) {
        return new Clock() {
            @Override
            public ZoneId getZone() {
                return zoneId;
            }

            @Override
            public Clock withZone(ZoneId zone) {
                return provideClock(zoneId);
            }

            @Override
            public Instant instant() {
                if (stoppedInstant == null) {
                    return ZonedDateTime.now(zoneId).toInstant();
                } else {
                    return stoppedInstant;
                }
            }
        };
    }
}
