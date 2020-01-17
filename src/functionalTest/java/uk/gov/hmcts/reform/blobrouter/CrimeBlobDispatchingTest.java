package uk.gov.hmcts.reform.blobrouter;

import com.azure.storage.blob.BlobServiceClient;
import com.azure.storage.blob.BlobServiceClientBuilder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import java.util.concurrent.TimeUnit;

import static com.google.common.io.Resources.getResource;
import static com.google.common.io.Resources.toByteArray;
import static com.jayway.awaitility.Awaitility.await;
import static java.util.Arrays.asList;
import static uk.gov.hmcts.reform.blobrouter.envelope.ZipFileHelper.createZipArchive;
import static uk.gov.hmcts.reform.blobrouter.storage.StorageHelper.blobExists;
import static uk.gov.hmcts.reform.blobrouter.storage.StorageHelper.uploadFile;

public class CrimeBlobDispatchingTest extends FunctionalTestBase {

    private BlobServiceClient crimeStorageClient;

    @Override
    @BeforeEach
    protected void setUp() {
        super.setUp();

        this.crimeStorageClient = new BlobServiceClientBuilder()
            .connectionString(config.crimeStorageConnectionString)
            .buildClient();
    }

    @Disabled("Functionality not implemented, yet")
    @Test
    void should_move_extracted_crime_envelope_to_crime_storage() throws Exception {
        // upload crime file with unique name
        String fileName = randomFileName();

        byte[] internalZipContent = toByteArray(
            getResource("test-data/envelope/crime/envelope.zip")
        );

        byte[] wrappingZipContent = createZipArchive(
            asList("test-data/envelope/crime/envelope.zip", "test-data/envelope/crime/signature")
        );

        // when
        uploadFile(blobRouterStorageClient, config.crimeDestinationContainer, fileName, wrappingZipContent);

        // then
        await("Wait for the blob to disappear from source container")
            .atMost(2, TimeUnit.MINUTES)
            .until(
                () -> !blobExists(blobRouterStorageClient, config.crimeSourceContainer, fileName)
            );

        assertBlobIsPresentInStorage(
            crimeStorageClient,
            config.crimeDestinationContainer,
            fileName,
            internalZipContent
        );
    }
}
