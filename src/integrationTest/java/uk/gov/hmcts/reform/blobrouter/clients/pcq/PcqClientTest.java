package uk.gov.hmcts.reform.blobrouter.clients.pcq;

import com.github.tomakehurst.wiremock.client.WireMock;
import feign.FeignException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.contract.wiremock.AutoConfigureWireMock;
import org.springframework.test.context.ActiveProfiles;
import uk.gov.hmcts.reform.blobrouter.clients.response.SasTokenResponse;

import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.notFound;
import static com.github.tomakehurst.wiremock.client.WireMock.okJson;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.unauthorized;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowableOfType;

@AutoConfigureWireMock(port = 0)
@ActiveProfiles("integration-test")
@SpringBootTest(properties = "pcq-backend-api-url=http://localhost:${wiremock.server.port}")
public class PcqClientTest {

    @Autowired
    private PcqClient client;

    @Test
    public void should_return_sas_token_when_service_authorization_header_is_valid() {

        var expected = new SasTokenResponse("pcq-sas-token");

        // given
        stubFor(get("/pcq/backend/token/bulkscan").willReturn(okJson("{\"sas_token\":\"pcq-sas-token\"}")));

        // when
        var sasResponse = client.getSasToken("Bearer valid-auth-token");

        // then
        assertThat(sasResponse).isEqualToComparingFieldByField(expected);
    }

    @Test
    public void should_throw_unauthorized_exception_when_service_authorization_header_is_invalid() {
        // given
        stubFor(get("/pcq/backend/token/bulkscan").willReturn(unauthorized()));

        // when
        FeignException.Unauthorized exception = catchThrowableOfType(
            () -> client.getSasToken("invalid-auth-token"),
            FeignException.Unauthorized.class
        );

        // then
        assertThat(exception.status()).isEqualTo(401);
    }

    @Test
    public void should_throw_not_found_exception_when_service_is_not_configured() {
        // given
        stubFor(get("/pcq/backend/token/bulkscan").willReturn(notFound()));

        // when
        FeignException.NotFound exception = catchThrowableOfType(
            () -> client.getSasToken("auth-token-for-unconfigured-service"),
            FeignException.NotFound.class
        );
        // then
        assertThat(exception.status()).isEqualTo(404);
    }

    @Test
    public void should_throw_server_exception_when_server_returns_server_error() {
        // given
        stubFor(get("/pcq/backend/token/bulkscan").willReturn(WireMock.serverError()));

        // when
        FeignException.InternalServerError exception = catchThrowableOfType(
            () -> client.getSasToken("auth-token-for-unconfigured-service"),
            FeignException.InternalServerError.class
        );
        // then
        assertThat(exception.status()).isEqualTo(500);
    }

}
