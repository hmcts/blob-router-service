package uk.gov.hmcts.reform.blobrouter;

import com.fasterxml.jackson.core.JsonProcessingException;
import io.restassured.RestAssured;
import io.restassured.response.Response;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.http.HttpStatus.OK;
import static org.springframework.http.HttpStatus.UNAUTHORIZED;

@ExtendWith(SpringExtension.class)
public class GetSasTokenTest extends OauthBase {

    private static final String SAS_TOKEN_ENDPOINT_PATH = "/token/bulkscan";

    @Test
    void should_accept_request_with_valid_jwt_token() throws Exception {
        String jwtAccessToken = getJwtAccessToken();
        assertThat(jwtAccessToken).isNotEmpty();

        Response response = callSasTokenEndpointWithJwt(jwtAccessToken);
        assertThat(response.getStatusCode()).isEqualTo(OK.value());
        assertThat(response.body().jsonPath().getString("sas_token")).isNotEmpty();
    }

    @Test
    void should_not_accept_request_without_valid_jwt_token() throws Exception {
        Response response = callSasTokenEndpointWithoutJwt();

        assertThat(response.getStatusCode()).isEqualTo(UNAUTHORIZED.value());
        assertThat(response.body().asString()).contains("Unauthorized. Access token is missing or invalid.");
    }

    @Test
    void should_reject_request_with_invalid_jwt_token() throws Exception {
        Response response = callSasTokenEndpointWithJwt("imnotarealaccesstoken");
        assertThat(response.statusCode()).isEqualTo(UNAUTHORIZED.value());
        assertThat(response.body().asString()).contains("Unauthorized. Access token is missing or invalid.");
    }

    private Response callSasTokenEndpointWithJwt(String jwtAccessToken) throws JsonProcessingException {
        return RestAssured.given().header("Authorization", jwtAccessToken)
            .get(getOauthApiGatewayUrl() + SAS_TOKEN_ENDPOINT_PATH)
            .thenReturn();
    }

    private Response callSasTokenEndpointWithoutJwt() {
        return RestAssured.given()
            .get(getOauthApiGatewayUrl() + SAS_TOKEN_ENDPOINT_PATH)
            .thenReturn();
    }
}
