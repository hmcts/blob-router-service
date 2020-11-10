package uk.gov.hmcts.reform.blobrouter;

import io.restassured.RestAssured;
import io.restassured.response.Response;
import io.restassured.specification.RequestSpecification;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.http.HttpStatus.NOT_FOUND;
import static org.springframework.http.HttpStatus.OK;
import static org.springframework.http.HttpStatus.UNAUTHORIZED;

public class GetSasTokenTest extends ApiGatewayBaseTest {

    private static final String SAS_TOKEN_ENDPOINT_PATH = "/token/bulkscan";

    @BeforeAll
    static void setup() throws Exception {
        loadConfig();
    }

    @Test
    @Disabled
    void should_accept_request_with_valid_certificate_and_subscription_key() throws Exception {
        Response response =
            callSasTokenEndpoint(validClientKeyStore, validSubscriptionKey)
                .thenReturn();

        assertThat(response.getStatusCode()).isEqualTo(OK.value());
        assertThat(response.body().jsonPath().getString("sas_token")).isNotEmpty();
    }

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

    @Test
    void should_reject_request_with_unrecognised_client_certificate() throws Exception {
        Response response = callSasTokenEndpoint(
            getUnrecognisedClientKeyStore(),
            validSubscriptionKey
        )
            .thenReturn();

        assertThat(response.statusCode()).isEqualTo(UNAUTHORIZED.value());
        assertThat(response.body().asString()).isEqualTo("Invalid client certificate");
    }

    @Test
    void should_reject_request_lacking_client_certificate() throws Exception {
        Response response =
            callSasTokenEndpoint(
                null,
                validSubscriptionKey
            )
                .thenReturn();

        assertThat(response.statusCode()).isEqualTo(UNAUTHORIZED.value());
        assertThat(response.body().asString()).isEqualTo("Missing client certificate");
    }

    @Test
    void should_not_expose_http_version() {
        Response response = RestAssured
            .given()
            .baseUri(apiGatewayUrl.replace("https://", "http://"))
            .header(SUBSCRIPTION_KEY_HEADER_NAME, validSubscriptionKey)
            .when()
            .get(SAS_TOKEN_ENDPOINT_PATH)
            .thenReturn();

        assertThat(response.statusCode()).isEqualTo(NOT_FOUND.value());
        assertThat(response.body().asString()).contains("Resource not found");
    }

    @Test
    void should_reject_request_with_expired_client_certificate() throws Exception {
        Response response = callSasTokenEndpoint(
            getExpiredClientKeyStore(),
            validSubscriptionKey
        )
            .thenReturn();

        assertThat(response.statusCode()).isEqualTo(UNAUTHORIZED.value());
        assertThat(response.body().asString()).isEqualTo("Invalid client certificate");
    }

    @Test
    void should_reject_request_with_not_yet_valid_client_certificate() throws Exception {
        Response response = callSasTokenEndpoint(
            getNotYetValidClientKeyStore(),
            validSubscriptionKey
        )
            .thenReturn();

        assertThat(response.statusCode()).isEqualTo(UNAUTHORIZED.value());
        assertThat(response.body().asString()).isEqualTo("Invalid client certificate");
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
