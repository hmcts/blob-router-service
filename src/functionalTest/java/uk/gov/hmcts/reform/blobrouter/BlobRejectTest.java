package uk.gov.hmcts.reform.blobrouter;

import com.azure.storage.blob.BlobServiceClient;
import org.apache.commons.codec.binary.Hex;
import org.apache.commons.codec.digest.DigestUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import java.util.concurrent.TimeUnit;

import static com.jayway.awaitility.Awaitility.await;
import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;
import static uk.gov.hmcts.reform.blobrouter.data.envelopes.Status.REJECTED;
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
    @Disabled
    void should_reject_crime_envelope_with_invalid_signature() throws Exception {
        // upload crime file with unique name
        String fileName = "reject_crime" + randomFileName();
        byte[] wrappingZipContent =
            createZipArchive(
                asList(
                    "test-data/envelope/envelope.zip",
                    "test-data/envelope/invalid-signature"
                )
            );

        // when
        uploadFile(blobRouterStorageClient, config.crimeSourceContainer, fileName, wrappingZipContent);

        // then
        await("Wait for the blob to disappear from source container")
            .atMost(2, TimeUnit.MINUTES)
            .until(() -> !blobExists(blobRouterStorageClient, config.crimeSourceContainer, fileName));

        // and
        assertFileInfoIsStored(fileName, config.crimeSourceContainer, REJECTED, true);

        assertBlobIsPresentInStorage(
            blobRouterStorageClient,
            "crime-rejected",
            fileName,
            wrappingZipContent
        );
    }

    @Test
    @Disabled
    void should_reject_bulkscan_envelope_without_signature() throws Exception {
        // upload crime file with unique name
        String fileName = "reject_bulkscan" + randomFileName();

        byte[] wrappingZipContent = createZipArchive(asList("test-data/envelope/envelope.zip"));

        // when
        uploadFile(blobRouterStorageClient, BULK_SCAN_CONTAINER, fileName, wrappingZipContent);

        // then
        await("Wait for the blob to disappear from source container")
            .atMost(2, TimeUnit.MINUTES)
            .until(() -> !blobExists(blobRouterStorageClient, BULK_SCAN_CONTAINER, fileName));

        // and
        assertFileInfoIsStored(fileName, BULK_SCAN_CONTAINER, REJECTED, true);

        assertBlobIsPresentInStorage(
            blobRouterStorageClient,
            "bulkscan-rejected",
            fileName,
            wrappingZipContent
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
