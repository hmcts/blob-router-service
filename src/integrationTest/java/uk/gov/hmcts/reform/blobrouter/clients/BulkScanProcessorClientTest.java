package uk.gov.hmcts.reform.blobrouter.clients;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.github.tomakehurst.wiremock.client.ResponseDefinitionBuilder;
import feign.FeignException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.contract.wiremock.AutoConfigureWireMock;
import org.springframework.test.context.ActiveProfiles;

import static com.github.tomakehurst.wiremock.client.WireMock.badRequest;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.okJson;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowableOfType;

@AutoConfigureWireMock(port = 0)
@ActiveProfiles("integration-test")
@SpringBootTest(properties = "bulk-scan-processor-url=http://localhost:${wiremock.server.port}")
public class BulkScanProcessorClientTest {

    @Autowired
    private BulkScanProcessorClient client;

    @Test
    public void should_return_sas_token_when_everything_is_ok() throws JsonProcessingException {

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
        // given
        stubWithResponse("notFoundService", badRequest());

        // when
        FeignException.BadRequest exception = catchThrowableOfType(
            () -> client.getSasToken("notFoundService"),
            FeignException.BadRequest.class
        );
        // then
        assertThat(exception.status()).isEqualTo(400);

    }

    private void stubWithResponse(String service, ResponseDefinitionBuilder builder) {
        stubFor(get("/token/" + service).willReturn(builder));
    }
}
