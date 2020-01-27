package uk.gov.hmcts.reform.blobrouter.clients.bulkscanprocessor;

import com.fasterxml.jackson.core.JsonProcessingException;
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

        var expected = new SasTokenResponse("187*2@(*&^%$£@12");

        // given
        stubFor(get("/token/sscs").willReturn(okJson("{\"sas_token\":\"187*2@(*&^%$£@12\"}")));

        // when
        SasTokenResponse sasResponse = client.getSasToken("sscs");

        // then
        assertThat(sasResponse).isEqualToComparingFieldByField(expected);
    }

    @Test
    public void should_throw_exception_when_requested_service_is_not_configured() throws Exception {
        // given
        stubFor(get("/token/notFoundService").willReturn(badRequest()));

        // when
        FeignException.BadRequest exception = catchThrowableOfType(
            () -> client.getSasToken("notFoundService"),
            FeignException.BadRequest.class
        );
        // then
        assertThat(exception.status()).isEqualTo(400);

    }

}
