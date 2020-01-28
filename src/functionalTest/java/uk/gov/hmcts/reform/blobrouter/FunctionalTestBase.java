package uk.gov.hmcts.reform.blobrouter;

import com.azure.storage.blob.BlobServiceClient;
import com.azure.storage.blob.BlobServiceClientBuilder;
import com.azure.storage.common.StorageSharedKeyCredential;
import org.apache.commons.codec.binary.Hex;
import org.apache.commons.codec.digest.DigestUtils;
import uk.gov.hmcts.reform.blobrouter.config.TestConfiguration;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.ThreadLocalRandom;

import static org.assertj.core.api.Assertions.assertThat;
import static uk.gov.hmcts.reform.blobrouter.storage.StorageHelper.blobExists;

public abstract class FunctionalTestBase {

    protected static TestConfiguration config = new TestConfiguration();

    private static final DateTimeFormatter FILE_NAME_DATE_TIME_FORMAT =
        DateTimeFormatter.ofPattern("dd-MM-yyyy-HH-mm-ss");

    protected BlobServiceClient blobRouterStorageClient;

    protected void setUp() {
        var blobRouterStorageCredential = new StorageSharedKeyCredential(
            config.sourceStorageAccountName,
            config.sourceStorageAccountKey
        );

        this.blobRouterStorageClient = new BlobServiceClientBuilder()
            .credential(blobRouterStorageCredential)
            .endpoint(config.sourceStorageAccountUrl)
            .buildClient();
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
                .getProperties()
                .getContentMd5()
        );

        String expectedBlobContentMd5 = DigestUtils.md5Hex(expectedContent);

        assertThat(blobContentMd5)
            .as("MD5 hash of the blob's content is as expected")
            .isEqualTo(expectedBlobContentMd5);
    }

    protected String randomFileName() {
        return String.format(
            "%s_%s.test.zip",
            ThreadLocalRandom.current().nextInt(0, Integer.MAX_VALUE),
            LocalDateTime.now().format(FILE_NAME_DATE_TIME_FORMAT)
        );
    }
}
