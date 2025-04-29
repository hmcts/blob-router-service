package uk.gov.hmcts.reform.blobrouter;

import io.restassured.RestAssured;
import io.restassured.response.Response;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.http.HttpStatus.OK;
import static org.springframework.http.HttpStatus.UNAUTHORIZED;

public class GetSasTokenTest extends ApiGatewayBaseTest {

    private static final String SAS_TOKEN_ENDPOINT_PATH = "/token/bulkscan";

    @BeforeAll
    static void setup() {
        loadConfig();
    }

    @Test
    void should_reject_request_with_missing_jwt_token() {
        Response response = callSasTokenEndpointWithoutJwt();

        assertThat(response.statusCode()).isEqualTo(UNAUTHORIZED.value());
        assertThat(response.body().asString()).contains("Unauthorized. Access token is missing or invalid.");
    }

    @Test
    void should_reject_request_with_invalid_jwt_token() {
        Response response = callSasTokenEndpointWithJwt("imnotarealaccesstoken");
        assertThat(response.statusCode()).isEqualTo(UNAUTHORIZED.value());
        assertThat(response.body().asString()).contains("Unauthorized. Access token is missing or invalid.");
    }

    @Test
    void should_accept_request_with_valid_jwt_token() {
        String jwtAccessToken = getJwtAccessToken();
        assertThat(jwtAccessToken).isNotEmpty();

        Response response = callSasTokenEndpointWithJwt(jwtAccessToken);
        assertThat(response.getStatusCode()).isEqualTo(OK.value());
        assertThat(response.body().jsonPath().getString("sas_token")).isNotEmpty();
    }

    private Response callSasTokenEndpointWithJwt(String jwtAccessToken) {
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
