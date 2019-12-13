package uk.gov.hmcts.reform.blobrouter;

import com.azure.storage.blob.BlobAsyncClient;
import io.restassured.RestAssured;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import uk.gov.hmcts.reform.logging.appinsights.SyntheticHeaders;

import java.io.File;
import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;
import static uk.gov.hmcts.reform.blobrouter.util.StorageHelper.ACCOUNT_URL;
import static uk.gov.hmcts.reform.blobrouter.util.StorageHelper.CONFIG;
import static uk.gov.hmcts.reform.blobrouter.util.StorageHelper.CONTAINER_CLIENT;
import static uk.gov.hmcts.reform.blobrouter.util.StorageHelper.CONTAINER_NAME;
import static uk.gov.hmcts.reform.blobrouter.util.StorageHelper.GET_BLOB_CLIENT;

class UploadAndDeleteBlobTest {

    private static final String TEST_URL = CONFIG.getString("test-url");

    @DisplayName("Upload using library client and delete afterwards")
    @Test
    void should_upload_and_delete_using_client() {
        // given
        File tmpFile = getFileToUpload();

        BlobAsyncClient blobClient = GET_BLOB_CLIENT.apply(tmpFile.getName());

        // when
        blobClient.uploadFromFile(tmpFile.getPath()).block();

        // then
        assertThat(CONTAINER_CLIENT.listBlobs().blockFirst().getName()).isEqualTo(tmpFile.getName());

        blobClient.delete().block();
    }

    @DisplayName("Upload using REST api and delete afterwards")
    @Test
    void should_upload_using_sas_token_and_delete_afterwards() {
        // given
        String sasToken = RestAssured
            .given()
            .relaxedHTTPSValidation()
            .baseUri(TEST_URL)
            .header(SyntheticHeaders.SYNTHETIC_TEST_SOURCE, "Blob router functional test")
            .get("/token/{containerName}", CONTAINER_NAME)
            .thenReturn()
            .getBody()
            .jsonPath()
            .getString("sas_token");

        File tmpFile = getFileToUpload();

        // when
        RestAssured
            .given()
            .relaxedHTTPSValidation()
            .basePath(ACCOUNT_URL)
            .header(SyntheticHeaders.SYNTHETIC_TEST_SOURCE, "Blob router functional test")
            .put("/{containerName}/{fileName}", CONTAINER_NAME, tmpFile.getName())
            .then()
            .statusCode(HttpStatus.CREATED.value());

        // then
        assertThat(CONTAINER_CLIENT.listBlobs().blockFirst().getName()).isEqualTo(tmpFile.getName());

        GET_BLOB_CLIENT.apply(tmpFile.getName()).delete().block();
    }

    private File getFileToUpload() {
        try {
            return File.createTempFile("fun", "test");
        } catch (IOException exception) {
            Assertions.fail("Failed to create temporary file", exception);

            throw new RuntimeException(exception);
        }
    }
}
