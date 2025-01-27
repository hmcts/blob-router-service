package uk.gov.hmcts.reform.blobrouter;

import io.restassured.RestAssured;
import io.restassured.response.Response;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.UNAUTHORIZED;

@ExtendWith(SpringExtension.class)
public class ReconciliationApiTest extends OauthBase {

    private static final String RECONCILIATION_ENDPOINT_PATH = "/reconciliation-report/{date}";

    @Test
    void should_reject_request_with_missing_jwt_token() {
        Response response = callReconciliationEndpointWithoutJwt();

        assertThat(response.statusCode()).isEqualTo(UNAUTHORIZED.value());
        assertThat(response.body().asString()).contains("Unauthorized. Access token is missing or invalid.");
    }

    @Test
    void should_reject_request_with_invalid_jwt_token() {
        Response response = callReconciliationEndpointWithJwt("imnotarealaccesstoken");
        assertThat(response.statusCode()).isEqualTo(UNAUTHORIZED.value());
        assertThat(response.body().asString()).contains("Unauthorized. Access token is missing or invalid.");
    }

    @Test
    void should_accept_request_with_valid_jwt_token() {
        String jwtAccessToken = getJwtAccessToken();
        assertThat(jwtAccessToken).isNotEmpty();

        Response response = callReconciliationEndpointWithJwt(jwtAccessToken);
        assertThat(response.getStatusCode()).isEqualTo(BAD_REQUEST.value());
    }

    private Response callReconciliationEndpointWithJwt(String jwtAccessToken) {
        String statementsReport = "{\"test\": {}}";
        return RestAssured.given().header("Authorization", jwtAccessToken)
            .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
            .body(statementsReport)
            .post(getOauthApiGatewayUrl() + RECONCILIATION_ENDPOINT_PATH, LocalDate.now().toString())
            .thenReturn();
    }

    private Response callReconciliationEndpointWithoutJwt() {
        String statementsReport = "{\"test\": {}}";
        return RestAssured.given()
            .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
            .body(statementsReport)
            .post(getOauthApiGatewayUrl() + RECONCILIATION_ENDPOINT_PATH, LocalDate.now().toString())
            .thenReturn();
    }
}
