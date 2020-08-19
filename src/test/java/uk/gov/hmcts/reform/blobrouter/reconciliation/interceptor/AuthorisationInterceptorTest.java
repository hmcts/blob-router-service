package uk.gov.hmcts.reform.blobrouter.reconciliation.interceptor;

import org.apache.commons.lang3.StringUtils;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.reform.blobrouter.exceptions.InvalidApiKeyException;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import static com.microsoft.applicationinsights.boot.dependencies.google.common.net.HttpHeaders.AUTHORIZATION;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
class AuthorisationInterceptorTest {

    @Mock private HttpServletRequest request;
    @Mock private HttpServletResponse response;
    @Mock private Object handler;

    private static String VALID_API_KEY = "test-valid-key";

    private static AuthorisationInterceptor authorisationInterceptor = new AuthorisationInterceptor(VALID_API_KEY);

    @Test
    void should_not_return_exception_when_the_authorisation_header_is_valid() {
        // given
        given(request.getHeader(AUTHORIZATION)).willReturn("Bearer " + VALID_API_KEY);

        // when
        var exception = catchThrowable(
            () -> authorisationInterceptor.preHandle(request, response, handler)
        );

        // then
        assertThat(exception).isNull();
    }

    @Test
    void should_return_exception_when_the_authorisation_header_is_null() {
        // given
        given(request.getHeader(AUTHORIZATION)).willReturn(null);

        // when
        var exception = catchThrowable(
            () -> authorisationInterceptor.preHandle(request, response, handler)
        );

        // then
        assertThat(exception).isInstanceOf(InvalidApiKeyException.class)
            .hasMessage("API Key is missing");
    }

    @ParameterizedTest
    @ValueSource(strings = {"", " ", "test-valid-key", "Bearer invalid-key"})
    void should_return_exception_when_the_authorisation_header_is_invalid(String authHeaderValue) {
        // given
        given(request.getHeader(AUTHORIZATION)).willReturn(authHeaderValue);
        String expectedMessage = StringUtils.isEmpty(authHeaderValue) ? "API Key is missing" : "Invalid API Key";

        // when
        var exception = catchThrowable(
            () -> authorisationInterceptor.preHandle(request, response, handler)
        );

        // then
        assertThat(exception).isInstanceOf(InvalidApiKeyException.class)
            .hasMessage(expectedMessage);
    }
}
