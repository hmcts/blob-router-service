package uk.gov.hmcts.reform.blobrouter;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static com.jayway.awaitility.Awaitility.await;
import static com.jayway.awaitility.Duration.TWO_MINUTES;
import static java.util.Arrays.asList;
import static uk.gov.hmcts.reform.blobrouter.data.model.Status.REJECTED;
import static uk.gov.hmcts.reform.blobrouter.envelope.ZipFileHelper.createZipArchive;
import static uk.gov.hmcts.reform.blobrouter.storage.StorageHelper.uploadFile;

public class BlobRejectTest extends FunctionalTestBase {

    @Override
    @BeforeEach
    protected void setUp() {
        super.setUp();
    }

    @Test
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
        await("Wait for the file to be processed")
            .atMost(TWO_MINUTES)
            .until(() -> fileWasProcessed(fileName, config.crimeSourceContainer));

        assertFileStatus(fileName, config.crimeSourceContainer, REJECTED);
    }

    @Test
    void should_reject_bulkscan_envelope_without_signature() throws Exception {
        // upload crime file with unique name
        String fileName = "reject_bulkscan" + randomFileName();

        byte[] wrappingZipContent = createZipArchive(asList("test-data/envelope/envelope.zip"));

        // when
        uploadFile(blobRouterStorageClient, BULK_SCAN_CONTAINER, fileName, wrappingZipContent);

        // then
        await("Wait for the file to be processed")
            .atMost(TWO_MINUTES)
            .until(() -> fileWasProcessed(fileName, BULK_SCAN_CONTAINER));

        assertFileStatus(fileName, BULK_SCAN_CONTAINER, REJECTED);
    }
}
