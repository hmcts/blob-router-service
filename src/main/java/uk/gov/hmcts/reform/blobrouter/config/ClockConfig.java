package uk.gov.hmcts.reform.blobrouter.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import uk.gov.hmcts.reform.blobrouter.util.TimeZones;

import java.time.Clock;
import javax.validation.ClockProvider;

/**
 * The `ClockConfig` class creates a bean for providing a clock set to the Europe/London time zone.
 */
@Configuration
public class ClockConfig {

    /**
     * The function creates a bean for providing a clock set to the Europe/London time zone.
     *
     * @return A ClockProvider bean is being returned, which is a functional interface that provides a Clock instance.
     *      The Clock instance returned by the lambda expression is created using the system clock with the time
     *      zone set to Europe/London.
     */
    @Bean
    public ClockProvider clockProvider() {
        return () -> Clock.system(TimeZones.EUROPE_LONDON_ZONE_ID);
    }
}
