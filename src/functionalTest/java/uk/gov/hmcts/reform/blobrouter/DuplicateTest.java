package uk.gov.hmcts.reform.blobrouter;

import io.restassured.RestAssured;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import uk.gov.hmcts.reform.blobrouter.data.events.EventType;

import java.util.concurrent.TimeUnit;

import static com.jayway.awaitility.Awaitility.await;
import static java.util.Arrays.asList;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasItem;
import static org.springframework.http.HttpStatus.OK;
import static uk.gov.hmcts.reform.blobrouter.data.envelopes.Status.DISPATCHED;
import static uk.gov.hmcts.reform.blobrouter.data.envelopes.Status.REJECTED;
import static uk.gov.hmcts.reform.blobrouter.envelope.ZipFileHelper.createZipArchive;
import static uk.gov.hmcts.reform.blobrouter.storage.StorageHelper.blobExists;
import static uk.gov.hmcts.reform.blobrouter.storage.StorageHelper.uploadFile;

public class DuplicateTest extends FunctionalTestBase {

    @Override
    @BeforeEach
    protected void setUp() {
        super.setUp();
    }

    @Test
    void should_reject_duplicate_envelope() throws Exception {
        // given
        final String fileName = randomFileName();

        byte[] content =
            createZipArchive(
                asList(
                    "test-data/envelope/envelope.zip",
                    "test-data/envelope/invalid-signature"
                )
            );

        // and
        uploadFile(blobRouterStorageClient, BULK_SCAN_CONTAINER, fileName, content);
        await("Wait first blob " + fileName + " to disappear from source container")
            .atMost(2, TimeUnit.MINUTES)
            .until(() -> !blobExists(blobRouterStorageClient, BULK_SCAN_CONTAINER, fileName));

        // when
        // upload same file again
        uploadFile(blobRouterStorageClient, BULK_SCAN_CONTAINER, fileName, content);

        // then
        await("Wait second blob " + fileName + " to disappear from source container")
            .atMost(2, TimeUnit.MINUTES)
            .until(() -> !blobExists(blobRouterStorageClient, BULK_SCAN_CONTAINER, fileName));

        // and
        assertEnvelopeIsRejected(fileName);
    }

    private void assertEnvelopeIsRejected(String fileName) {
        RestAssured
            .given()
            .baseUri(config.blobRouterUrl)
            .relaxedHTTPSValidation()
            .queryParam("file_name", fileName)
            .queryParam("container", BULK_SCAN_CONTAINER)
            .get("/envelopes")
            .then()
            .statusCode(OK.value())
            .body("data[0].status", equalTo(REJECTED.name()))
            .body(
                "data[0].events.event", hasItem(EventType.REJECTED.name())
            )
            .body("data[1].status", equalTo(DISPATCHED.name()))
            .body(
                "data[1].events.event", hasItem(EventType.DISPATCHED.name())
            );
    }
}
