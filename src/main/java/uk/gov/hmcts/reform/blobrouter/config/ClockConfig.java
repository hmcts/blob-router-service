package uk.gov.hmcts.reform.blobrouter.config;

import jakarta.validation.ClockProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import uk.gov.hmcts.reform.blobrouter.util.TimeZones;

import java.time.Clock;

@Configuration
public class ClockConfig {

    @Bean
    public ClockProvider clockProvider() {
        return () -> Clock.system(TimeZones.EUROPE_LONDON_ZONE_ID);
    }
}
