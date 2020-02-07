package uk.gov.hmcts.reform.blobrouter;

import com.azure.storage.blob.BlobServiceClient;
import io.restassured.RestAssured;
import org.apache.commons.codec.binary.Hex;
import org.apache.commons.codec.digest.DigestUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.concurrent.TimeUnit;

import static com.google.common.io.Resources.getResource;
import static com.google.common.io.Resources.toByteArray;
import static com.jayway.awaitility.Awaitility.await;
import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.springframework.http.HttpStatus.OK;
import static uk.gov.hmcts.reform.blobrouter.data.model.Status.REJECTED;
import static uk.gov.hmcts.reform.blobrouter.envelope.ZipFileHelper.createZipArchive;
import static uk.gov.hmcts.reform.blobrouter.storage.StorageHelper.blobExists;
import static uk.gov.hmcts.reform.blobrouter.storage.StorageHelper.uploadFile;

public class BlobRejectTest extends FunctionalTestBase {

    @Override
    @BeforeEach
    protected void setUp() {
        super.setUp();
    }

    @Test
    void should_reject_envelope_without_signature() throws Exception {
        // upload crime file with unique name
        String fileName = "should_reject_" + randomFileName();

        byte[] wrappingZipContent = createZipArchive(
                asList("test-data/envelope/envelope.zip","test-data/envelope/invalid-signature")
        );

        // when
        uploadFile(blobRouterStorageClient, config.crimeSourceContainer, fileName, wrappingZipContent);

        // then
        await("Wait for the blob to disappear from source container")
                .atMost(2, TimeUnit.MINUTES)
                .until(() -> !blobExists(blobRouterStorageClient, config.crimeSourceContainer, fileName));

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

        byte[] expectedContent = toByteArray(getResource("test-data/envelope/envelope.zip"));

        System.out.println("expectedContent.length=" + expectedContent.length);
        System.out.println("expectedContent. value=" + new String(expectedContent, StandardCharsets.UTF_8));
        assertBlobIsPresentInStorage(
                blobRouterStorageClient,
                "crime-rejected",
                fileName,
                expectedContent
        );
    }

    protected void assertBlobIsPresentInStorage(
            BlobServiceClient client,
            String containerName,
            String fileName,
            byte[] expectedContent
    ) {
        assertThat(blobExists(client, containerName, fileName))
                .as("File %s exists in container %s", fileName, containerName)
                .isTrue();

        String blobContentMd5 = Hex.encodeHexString(
                client
                        .getBlobContainerClient(containerName)
                        .getBlobClient(fileName)
                        .getBlockBlobClient()
                        .getProperties()
                        .getContentMd5()
        );

        String expectedBlobContentMd5 = DigestUtils.md5Hex(expectedContent);

        assertThat(blobContentMd5)
                .as("MD5 hash of the blob's content is as expected")
                .isEqualTo(expectedBlobContentMd5);
    }

}
