package uk.gov.hmcts.reform.blobrouter;

import io.restassured.RestAssured;
import io.restassured.response.Response;
import io.restassured.specification.RequestSpecification;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import uk.gov.hmcts.reform.logging.appinsights.SyntheticHeaders;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.UNAUTHORIZED;

public class ReconciliationApiTest extends ApiGatewayBaseTest {

    private static final String RECONCILIATION_ENDPOINT_PATH = "/reconciliation-report/{date}";

    @BeforeAll
    static void setup() throws Exception {
        loadConfig();
    }

    @Disabled
    @Test
    void should_accept_request_with_valid_certificate_and_valid_subscription_key() throws Exception {
        Response response = callReconciliationEndpoint(validClientKeyStore, validSubscriptionKey);

        assertThat(response.getStatusCode()).isEqualTo(BAD_REQUEST.value());
    }

    @Disabled
    @Test
    void should_reject_request_with_invalid_subscription_key() throws Exception {
        Response response = callReconciliationEndpoint(validClientKeyStore, "invalid-subscription-key");

        assertThat(response.statusCode()).isEqualTo(UNAUTHORIZED.value());
        assertThat(response.body().asString()).contains("Access denied due to invalid subscription key");
    }

    @Disabled
    @Test
    void should_reject_request_with_missing_subscription_header() throws Exception {
        Response response = callReconciliationEndpoint(validClientKeyStore, null);

        assertThat(response.statusCode()).isEqualTo(UNAUTHORIZED.value());
        assertThat(response.body().asString()).contains("Access denied due to missing subscription key");
    }

    private Response callReconciliationEndpoint(
        KeyStoreWithPassword clientKeyStore,
        String subscriptionKey
    ) throws Exception {
        String statementsReport = "{\"test\": {}}";
        RequestSpecification request = RestAssured
            .given()
            .baseUri(apiGatewayUrl)
            .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
            .header(SyntheticHeaders.SYNTHETIC_TEST_SOURCE, "Blob Router Service Smoke test")
            .body(statementsReport);

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
        return request.post(RECONCILIATION_ENDPOINT_PATH, LocalDate.now().toString());
    }

}
