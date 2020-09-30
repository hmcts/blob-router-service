package uk.gov.hmcts.reform.blobrouter.config;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import uk.gov.hmcts.reform.blobrouter.util.TimeZones;

import javax.validation.ClockProvider;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;

@TestConfiguration
public class TestClockProvider {

    public static Instant stoppedInstant = Instant.now();

    @Bean
    @Primary
    public ClockProvider stoppedClock() {
        return () -> provideClock(TimeZones.EUROPE_LONDON_ZONE_ID);
    }

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
                return stoppedInstant;
            }
        };
    }
}
