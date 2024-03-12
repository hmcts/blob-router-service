package uk.gov.hmcts.reform.blobrouter.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import uk.gov.hmcts.reform.authorisation.generators.AuthTokenGenerator;

import static org.mockito.Mockito.mock;

/**
 * The `AuthConfig` class creates a mock instance of `AuthTokenGenerator` bean based on a specific property value.
 */
public class AuthConfig {

    /**
     * This function creates a bean for an AuthTokenGenerator only if a specific property is set to "false".
     *
     * @return A mock instance of the `AuthTokenGenerator` class is being returned.
     */
    @Bean
    @ConditionalOnProperty(name = "idam.s2s-auth.url", havingValue = "false")
    public AuthTokenGenerator authTokenGenerator() {
        return mock(AuthTokenGenerator.class);
    }
}
