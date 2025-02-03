package uk.gov.hmcts.reform.blobrouter;

import com.fasterxml.jackson.core.JsonProcessingException;
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

    //APIM Certificate Tests (OLD)
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

    //APIM OAUTH Tests
    @Test
    void should_reject_request_with_missing_jwt_token() throws Exception {
        Response response = callSasTokenEndpointWithoutJwt();

        assertThat(response.statusCode()).isEqualTo(UNAUTHORIZED.value());
        assertThat(response.body().asString()).contains("Unauthorized. Access token is missing or invalid.");
    }

    @Test
    void should_reject_request_with_invalid_jwt_token() throws Exception {
        Response response = callSasTokenEndpointWithJwt("imnotarealaccesstoken");
        assertThat(response.statusCode()).isEqualTo(UNAUTHORIZED.value());
        assertThat(response.body().asString()).contains("Unauthorized. Access token is missing or invalid.");
    }

    @Test
    void should_accept_request_with_valid_jwt_token() throws Exception {
        String jwtAccessToken = getJwtAccessToken();
        assertThat(jwtAccessToken).isNotEmpty();

        Response response = callSasTokenEndpointWithJwt(jwtAccessToken);
        assertThat(response.getStatusCode()).isEqualTo(OK.value());
        assertThat(response.body().jsonPath().getString("sas_token")).isNotEmpty();
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

    private Response callSasTokenEndpointWithJwt(String jwtAccessToken) throws JsonProcessingException {
        Response response = RestAssured.given().header("Authorization", jwtAccessToken)
            .get(oauthGatewayUrl + SAS_TOKEN_ENDPOINT_PATH)
            .thenReturn();
        return response;
    }

    private Response callSasTokenEndpointWithoutJwt() {
        Response response = RestAssured.given()
            .get(oauthGatewayUrl + SAS_TOKEN_ENDPOINT_PATH)
            .thenReturn();
        return response;
    }
}
