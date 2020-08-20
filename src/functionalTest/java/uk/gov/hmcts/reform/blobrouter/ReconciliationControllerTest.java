package uk.gov.hmcts.reform.blobrouter;

import com.google.common.io.Resources;
import io.restassured.RestAssured;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import uk.gov.hmcts.reform.blobrouter.config.TestConfiguration;
import uk.gov.hmcts.reform.logging.appinsights.SyntheticHeaders;

import java.io.IOException;
import java.time.LocalDate;
import java.util.UUID;

import static com.google.common.io.Resources.getResource;
import static java.nio.charset.StandardCharsets.UTF_8;
import static org.springframework.http.HttpStatus.OK;
import static org.springframework.http.HttpStatus.UNAUTHORIZED;

public class ReconciliationControllerTest {

    private static final TestConfiguration config = new TestConfiguration();

    @Test
    void should_return_success_response_when_authorization_key_is_valid() throws Exception {
        // given
        String validAuthKey = "Bearer " + config.reconciliationApiKey;

        // then
        postReconciliationSupplierReport(validAuthKey, OK.value());
    }

    @Test
    void should_return_unauthorized_response_when_authorization_key_is_invalid() throws Exception {
        // given
        String invalidAuthKey = "Bearer " + UUID.randomUUID().toString();

        // then
        postReconciliationSupplierReport(invalidAuthKey, UNAUTHORIZED.value());
    }

    private void postReconciliationSupplierReport(String authKey, int expectedStatus) throws IOException {
        String report = Resources.toString(
            getResource("reconciliation/reconciliation-supplier-report.json"),
            UTF_8
        );

        RestAssured
            .given()
            .relaxedHTTPSValidation()
            .baseUri(config.blobRouterUrl)
            .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
            .header(SyntheticHeaders.SYNTHETIC_TEST_SOURCE, "Blob Router Service Functional test")
            .header(HttpHeaders.AUTHORIZATION, authKey)
            .body(report)
            .post("/reform-scan/reconciliation-report/{date}", LocalDate.now().toString())
            .then()
            .statusCode(expectedStatus);
    }

}
