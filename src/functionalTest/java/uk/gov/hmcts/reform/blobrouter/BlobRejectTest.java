package uk.gov.hmcts.reform.blobrouter;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

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
        assertFileInfoIsStored(fileName, config.crimeSourceContainer, REJECTED, true);
    }

    @Test
    void should_reject_bulkscan_envelope_without_signature() throws Exception {
        // upload crime file with unique name
        String fileName = "reject_bulkscan" + randomFileName();

        byte[] wrappingZipContent = createZipArchive(asList("test-data/envelope/envelope.zip"));

        // when
        uploadFile(blobRouterStorageClient, BULK_SCAN_CONTAINER, fileName, wrappingZipContent);

        // then
        assertFileInfoIsStored(fileName, BULK_SCAN_CONTAINER, REJECTED, true);
    }
}
