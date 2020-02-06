package uk.gov.hmcts.reform.blobrouter;

import com.azure.storage.blob.BlobServiceClient;
import com.azure.storage.blob.BlobServiceClientBuilder;
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
import static uk.gov.hmcts.reform.blobrouter.data.model.Status.REJECTED;
import static uk.gov.hmcts.reform.blobrouter.envelope.ZipFileHelper.createZipArchive;
import static uk.gov.hmcts.reform.blobrouter.storage.StorageHelper.blobExists;
import static uk.gov.hmcts.reform.blobrouter.storage.StorageHelper.uploadFile;

public class BlobRejectTest extends FunctionalTestBase {

    private BlobServiceClient crimeStorageClient;

    @Override
    @BeforeEach
    protected void setUp() {
        super.setUp();

        this.crimeStorageClient = new BlobServiceClientBuilder()
            .connectionString(config.crimeStorageConnectionString)
            .buildClient();
    }

    @Test
    void should_reject_envelope_without_signature() throws Exception {
        // upload crime file with unique name
        String fileName = "will_reject_" + randomFileName();

        byte[] internalZipContent = toByteArray(
            getResource("test-data/envelope/envelope.zip")
        );

        byte[] wrappingZipContent = createZipArchive(
            asList("test-data/envelope/envelope.zip")
        );

        // when
        uploadFile(blobRouterStorageClient, config.crimeSourceContainer, fileName, wrappingZipContent);

        // then
        await("Wait for the blob to disappear from source container")
            .atMost(2, TimeUnit.MINUTES)
            .until(
                () -> !blobExists(blobRouterStorageClient, config.crimeSourceContainer, fileName)
            );

        // and
        RestAssured
            .given()
            .baseUri(config.blobRouterUrl)
            .relaxedHTTPSValidation()
            .queryParam("file_name", fileName)
            .queryParam("container", config.crimeSourceContainer)
            .get("/envelopes")
            .then()
            .statusCode(OK.value())
            .body("container", equalTo("crime"))
            .body("status", equalTo(REJECTED.name()))
            .body("is_deleted", equalTo(true));


        assertBlobIsPresentInStorage(
            blobRouterStorageClient,
            "crime-rejected",
            fileName,
            internalZipContent
        );
    }
}
