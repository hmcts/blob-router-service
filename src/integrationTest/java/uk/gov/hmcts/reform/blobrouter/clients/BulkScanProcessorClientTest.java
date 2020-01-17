package uk.gov.hmcts.reform.blobrouter.clients;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.github.tomakehurst.wiremock.client.ResponseDefinitionBuilder;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.contract.wiremock.AutoConfigureWireMock;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.web.client.HttpClientErrorException;

import static com.github.tomakehurst.wiremock.client.WireMock.badRequest;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.okJson;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowableOfType;
import static org.springframework.http.HttpStatus.BAD_REQUEST;

@AutoConfigureWireMock(port = 0)
@ActiveProfiles("integration-test")
@SpringBootTest(properties = "bulk-scan-processor-url=http://localhost:${wiremock.server.port}")
public class BulkScanProcessorClientTest {

    @Autowired
    private BulkScanProcessorClient client;

    @Test
    public void should_return_sas_token_with_when_everything_is_ok_with_request() throws JsonProcessingException {

        SasTokenResponse expected = new SasTokenResponse("187*2@(*&^%$£@12");

        // given
        stubWithResponse("sscs", okJson("{\"sas_token\":\"187*2@(*&^%$£@12\"}"));

        // when
        SasTokenResponse sasResponse = client.getSasToken("sscs");

        // then
        assertThat(sasResponse).isEqualToComparingFieldByField(expected);
    }

    @Test
    public void should_throw_exception_when_requested_service_is_not_configured() throws Exception {

        stubWithResponse("notFoundService", badRequest());

        // when
        HttpClientErrorException.BadRequest exception = catchThrowableOfType(
            () -> client.getSasToken("notFoundService"),
            HttpClientErrorException.BadRequest.class
        );

        assertThat(exception.getStatusCode()).isEqualTo(BAD_REQUEST);

    }

    private void stubWithResponse(String service, ResponseDefinitionBuilder builder) {
        stubFor(get("/token/" + service).willReturn(builder));
    }
}
