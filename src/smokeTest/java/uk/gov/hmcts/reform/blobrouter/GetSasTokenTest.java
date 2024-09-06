package uk.gov.hmcts.reform.blobrouter;

import io.restassured.RestAssured;
import io.restassured.response.Response;
import io.restassured.specification.RequestSpecification;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.http.HttpStatus.OK;
import static org.springframework.http.HttpStatus.UNAUTHORIZED;


public class GetSasTokenTest extends ApiGatewayBaseTest {

    private static final String SAS_TOKEN_ENDPOINT_PATH = "/token/bulkscan";

    @BeforeAll
    static void setup() throws Exception {
        loadConfig();
    }

    @Disabled
    @Test
    void should_accept_request_with_valid_certificate_and_subscription_key() throws Exception {
        Response response =
            callSasTokenEndpoint(validClientKeyStore, validSubscriptionKey)
                .thenReturn();

        assertThat(response.getStatusCode()).isEqualTo(OK.value());
        assertThat(response.body().jsonPath().getString("sas_token")).isNotEmpty();
    }

    @Disabled
    @Test
    void should_reject_request_with_invalid_subscription_key() throws Exception {
        Response response = callSasTokenEndpoint(
            validClientKeyStore,
            "invalid-subscription-key123"
        )
            .thenReturn();

        assertThat(response.statusCode()).isEqualTo(UNAUTHORIZED.value());
        assertThat(response.body().asString()).contains("Access denied due to invalid subscription key");
    }

    @Disabled
    @Test
    void should_reject_request_lacking_subscription_key() throws Exception {
        Response response = callSasTokenEndpoint(
            validClientKeyStore,
            null
        )
            .thenReturn();

        assertThat(response.statusCode()).isEqualTo(UNAUTHORIZED.value());
        assertThat(response.body().asString()).contains("Access denied due to missing subscription key");
    }


    private Response callSasTokenEndpoint(
        KeyStoreWithPassword clientKeyStore,
        String subscriptionKey
    ) throws Exception {
        RequestSpecification request = RestAssured.given().baseUri(apiGatewayUrl);

        if (clientKeyStore != null) {
            request = request.config(
                getSslConfigForClientCertificate(
                    clientKeyStore.keyStore,
                    clientKeyStore.password
                )
            );
        }

        if (subscriptionKey != null) {
            request = request.header(SUBSCRIPTION_KEY_HEADER_NAME, subscriptionKey);
        }

        return request.get(SAS_TOKEN_ENDPOINT_PATH);
    }

}
