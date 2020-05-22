package uk.gov.hmcts.reform.blobrouter;

import com.typesafe.config.ConfigFactory;
import io.restassured.RestAssured;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import uk.gov.hmcts.reform.logging.appinsights.SyntheticHeaders;

import static org.hamcrest.Matchers.containsString;

public class BlobStorageHealthTest {

    private static final String TEST_URL = ConfigFactory.load().getString("test-url");

    @Test
    @Disabled("temporarily")
    public void should_get_the_sas_token_for_service() {
        RestAssured
            .given()
            .relaxedHTTPSValidation()
            .baseUri(TEST_URL)
            .header(SyntheticHeaders.SYNTHETIC_TEST_SOURCE, "Blob router smoke test")
            .get("/token/bulkscan")
            .then()
            .statusCode(200)
            .body("sas_token", containsString("sig"));
    }
}
