package uk.gov.hmcts.reform.blobrouter.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;
import uk.gov.hmcts.reform.authorisation.ServiceAuthorisationApi;
import uk.gov.hmcts.reform.authorisation.generators.AuthTokenGenerator;
import uk.gov.hmcts.reform.authorisation.generators.AuthTokenGeneratorFactory;

/**
 * The `AuthConfig` class in Java defines a bean `authTokenGenerator` based on conditional properties and values for
 * generating authentication tokens.
 */
@Configuration
@Lazy
public class AuthConfig {

    /**
     * This function creates an AuthTokenGenerator bean based on certain conditional properties and values provided.
     *
     * @param secret The `secret` parameter is a String value that represents the secret key used for generating
     *      authentication tokens. It is typically a secure and confidential value that is known only to authorized
     *      parties for authentication purposes.
     * @param name The `name` parameter in the code snippet refers to the value of the property `idam.s2s-auth.name`.
     *      It is used as a parameter for creating an `AuthTokenGenerator` bean in the Spring application context.
     * @param serviceAuthorisationApi The `serviceAuthorisationApi` parameter in the `authTokenGenerator` method is of
     *      type `ServiceAuthorisationApi`. It is likely a bean or component that provides functionality related
     *      to service authorization within your application. This parameter is being injected into the
     *      `authTokenGenerator` bean creation method along with
     * @return An `AuthTokenGenerator` object is being returned.
     */
    @Bean
    @ConditionalOnProperty(name = "idam.s2s-auth.url")
    public AuthTokenGenerator authTokenGenerator(
        @Value("${idam.s2s-auth.secret}") String secret,
        @Value("${idam.s2s-auth.name}") String name,
        ServiceAuthorisationApi serviceAuthorisationApi
    ) {
        return AuthTokenGeneratorFactory.createDefaultGenerator(secret, name, serviceAuthorisationApi);
    }
}
