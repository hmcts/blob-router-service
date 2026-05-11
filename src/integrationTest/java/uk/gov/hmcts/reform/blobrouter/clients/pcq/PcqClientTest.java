package uk.gov.hmcts.reform.blobrouter.clients.pcq;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import uk.gov.hmcts.reform.blobrouter.clients.response.SasTokenResponse;

import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.okJson;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static org.assertj.core.api.Assertions.assertThat;

@ActiveProfiles("integration-test")
@SpringBootTest
public class PcqClientTest {

    static WireMockServer wireMockServer =
        new WireMockServer(WireMockConfiguration.options().dynamicPort());

    @BeforeAll
    static void setup() {
        wireMockServer.start();
        WireMock.configureFor("localhost", wireMockServer.port());
    }

    @AfterAll
    static void teardown() {
        wireMockServer.stop();
    }

    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry registry) {
        registry.add(
            "pcq-backend-api-url",
            () -> "http://localhost:" + wireMockServer.port()
        );
    }

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
