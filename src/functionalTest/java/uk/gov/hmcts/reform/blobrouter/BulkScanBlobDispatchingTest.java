package uk.gov.hmcts.reform.blobrouter;

import io.restassured.RestAssured;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.concurrent.TimeUnit;

import static com.google.common.io.Resources.getResource;
import static com.google.common.io.Resources.toByteArray;
import static com.jayway.awaitility.Awaitility.await;
import static java.util.Arrays.asList;
import static org.hamcrest.Matchers.equalTo;
import static org.springframework.http.HttpStatus.OK;
import static uk.gov.hmcts.reform.blobrouter.data.model.Status.DISPATCHED;
import static uk.gov.hmcts.reform.blobrouter.envelope.ZipFileHelper.createZipArchive;
import static uk.gov.hmcts.reform.blobrouter.storage.StorageHelper.blobExists;
import static uk.gov.hmcts.reform.blobrouter.storage.StorageHelper.uploadFile;

public class BulkScanBlobDispatchingTest extends FunctionalTestBase {

    private static final String CONTAINER = "bulkscan";

    @BeforeEach
    @Override
    protected void setUp() {
        super.setUp();
    }

    @Test
    void should_move_blob_to_bulkscan_storage() throws Exception {
        // upload bulkscan file with unique name
        String fileName = randomFileName();

        byte[] internalZipContent = toByteArray(
            getResource("test-data/envelope/envelope.zip")
        );

        byte[] wrappingZipContent = createZipArchive(
            asList("test-data/envelope/envelope.zip", "test-data/envelope/signature")
        );

        // when
        uploadFile(blobRouterStorageClient, CONTAINER, fileName, wrappingZipContent);

        // then
        await("Wait for the " + fileName + " to disappear from source container")
            .atMost(2, TimeUnit.MINUTES)
            .until(
                () -> !blobExists(blobRouterStorageClient, CONTAINER, fileName)
            );

        // and
        RestAssured
            .given()
            .baseUri(config.blobRouterUrl)
            .relaxedHTTPSValidation()
            .queryParam("file_name", fileName)
            .queryParam("container", CONTAINER)
            .get("/envelopes")
            .then()
            .statusCode(OK.value())
            .body("$.status", equalTo(DISPATCHED))
            .body("$.is_deleted", equalTo(true));
    }
}
