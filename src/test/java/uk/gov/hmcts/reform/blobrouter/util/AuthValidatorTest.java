package uk.gov.hmcts.reform.blobrouter.util;

import org.junit.jupiter.api.Test;
import uk.gov.hmcts.reform.blobrouter.exceptions.InvalidApiKeyException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatCode;

class AuthValidatorTest {

    @Test
    void should_throw_if_authorization_key_is_null() {
        assertThat(catchThrowable(
            () -> AuthValidator.validateAuthorization(null, "apikey"))
        )
        .isInstanceOf(InvalidApiKeyException.class)
            .hasMessage("API Key is missing");
    }

    @Test
    void should_throw_if_authorization_key_is_blank() {
        assertThat(catchThrowable(
            () -> AuthValidator.validateAuthorization("", "apikey"))
        )
        .isInstanceOf(InvalidApiKeyException.class)
            .hasMessage("API Key is missing");
    }

    @Test
    void should_throw_if_authorization_key_is_incorrect() {
        assertThat(catchThrowable(
            () -> AuthValidator.validateAuthorization("incorrect", "apikey"))
        )
        .isInstanceOf(InvalidApiKeyException.class)
            .hasMessage("Invalid API Key");
    }

    @Test
    void should_throw_if_authorization_key_does_not_have_prefix() {
        assertThat(catchThrowable(
            () -> AuthValidator.validateAuthorization("apikey", "apikey"))
        )
        .isInstanceOf(InvalidApiKeyException.class)
            .hasMessage("Invalid API Key");
    }

    @Test
    void should_not_throw_if_authorization_key_is_correct() {
        assertThatCode(
            () -> AuthValidator.validateAuthorization("Bearer apikey", "apikey")
        )
            .doesNotThrowAnyException();
    }
}