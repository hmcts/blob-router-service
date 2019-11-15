package uk.gov.hmcts.reform.blobrouter;

import io.restassured.RestAssured;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import uk.gov.hmcts.reform.logging.appinsights.SyntheticHeaders;

import static org.hamcrest.Matchers.containsString;

@SpringBootTest
public class BlobStorageHealthTest {

    @Value("${test-url}")
    private String testUrl;

    @Test
    public void should_get_the_sas_token_for_service() {
        RestAssured
            .given()
            .relaxedHTTPSValidation()
            .baseUri(testUrl)
            .header(SyntheticHeaders.SYNTHETIC_TEST_SOURCE, "Blob router smoke test")
            .get("/token/bulkscan")
            .then()
            .statusCode(200)
            .body("sas_token", containsString("sig"));
    }
}
