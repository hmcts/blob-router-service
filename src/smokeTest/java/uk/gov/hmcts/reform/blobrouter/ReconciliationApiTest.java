package uk.gov.hmcts.reform.blobrouter;

import io.restassured.RestAssured;
import io.restassured.response.Response;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import uk.gov.hmcts.reform.logging.appinsights.SyntheticHeaders;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.NOT_FOUND;
import static org.springframework.http.HttpStatus.UNAUTHORIZED;

public class ReconciliationApiTest extends ApiGatewayBaseTest {

    private static final String RECONCILIATION_ENDPOINT_PATH = "/reconciliation-report/{date}";

    @BeforeAll
    static void setup() throws Exception {
        loadConfig();
    }

    @Test
    void should_accept_request_with_valid_subscription_key() {
        Response response = callReconciliationEndpoint(validSubscriptionKey);

        assertThat(response.getStatusCode()).isEqualTo(BAD_REQUEST.value());
    }

    @Test
    void should_reject_request_with_invalid_subscription_key() {
        Response response = callReconciliationEndpoint("invalid-subscription-key");

        assertThat(response.statusCode()).isEqualTo(UNAUTHORIZED.value());
        assertThat(response.body().asString()).contains("Access denied due to invalid subscription key");
    }

    @Test
    void should_reject_request_with_missing_subscription_header() {
        String statementsReport = "{\"test\": {}}";
        Response response = RestAssured
            .given()
            .baseUri(apiGatewayUrl)
            .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
            .header(SyntheticHeaders.SYNTHETIC_TEST_SOURCE, "Blob Router Service Smoke test")
            .body(statementsReport)
            .post(RECONCILIATION_ENDPOINT_PATH, LocalDate.now().toString());

        assertThat(response.statusCode()).isEqualTo(UNAUTHORIZED.value());
        assertThat(response.body().asString()).contains("Access denied due to missing subscription key");
    }

    @Test
    void should_not_expose_http_version() {
        String statementsReport = "{\"test\": {}}";
        Response response = RestAssured
            .given()
            .baseUri(apiGatewayUrl.replace("https://", "http://"))
            .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
            .header(SUBSCRIPTION_KEY_HEADER_NAME, validSubscriptionKey)
            .header(SyntheticHeaders.SYNTHETIC_TEST_SOURCE, "Blob Router Service Smoke test")
            .body(statementsReport)
            .when()
            .post(RECONCILIATION_ENDPOINT_PATH, LocalDate.now().toString())
            .thenReturn();

        assertThat(response.statusCode()).isEqualTo(NOT_FOUND.value());
        assertThat(response.body().asString()).contains("Resource not found");
    }

    private Response callReconciliationEndpoint(String subscriptionKey) {
        String statementsReport = "{\"test\": {}}";
        return RestAssured
            .given()
            .baseUri(apiGatewayUrl)
            .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
            .header(SUBSCRIPTION_KEY_HEADER_NAME, subscriptionKey)
            .header(SyntheticHeaders.SYNTHETIC_TEST_SOURCE, "Blob Router Service Smoke test")
            .body(statementsReport)
            .post(RECONCILIATION_ENDPOINT_PATH, LocalDate.now().toString());
    }

}
