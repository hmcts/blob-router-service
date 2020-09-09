package uk.gov.hmcts.reform.blobrouter.clients.pcq;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.contract.wiremock.AutoConfigureWireMock;
import org.springframework.test.context.ActiveProfiles;
import uk.gov.hmcts.reform.blobrouter.clients.response.SasTokenResponse;

import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.okJson;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static org.assertj.core.api.Assertions.assertThat;

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
        stubFor(
            get("/pcq/backend/token/bulkscan")
                .withHeader("ServiceAuthorization", equalTo("Bearer valid-auth-token"))
                .willReturn(okJson("{\"sas_token\":\"pcq-sas-token\"}"))
        );

        // when
        var sasResponse = client.getSasToken("Bearer valid-auth-token");

        // then
        assertThat(sasResponse).isEqualToComparingFieldByField(expected);
    }

}
