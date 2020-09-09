package uk.gov.hmcts.reform.blobrouter.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import uk.gov.hmcts.reform.authorisation.generators.AuthTokenGenerator;

import static org.mockito.Mockito.mock;

public class AuthConfig {

    @Bean
    @ConditionalOnProperty(name = "idam.s2s-auth.url", havingValue = "false")
    public AuthTokenGenerator authTokenGenerator() {
        return mock(AuthTokenGenerator.class);
    }
}
