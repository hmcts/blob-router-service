package uk.gov.hmcts.reform.blobrouter;

import com.typesafe.config.ConfigFactory;
import io.restassured.RestAssured;
import org.junit.jupiter.api.Test;

import static org.hamcrest.Matchers.containsString;

public class BlobStorageHealthTest {

    private static final String TEST_URL = ConfigFactory.load().getString("test-url");
    private static final String SYNTHETIC_TEST_SOURCE_HEADER = "SyntheticTest-Source";

    @Test
    public void should_get_the_sas_token_for_service() {
        RestAssured
            .given()
            .relaxedHTTPSValidation()
            .baseUri(TEST_URL)
            .header(SYNTHETIC_TEST_SOURCE_HEADER, "Blob router smoke test")
            .get("/token/bulkscan")
            .then()
            .statusCode(200)
            .body("sas_token", containsString("sig"));
    }
}
