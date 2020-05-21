package uk.gov.hmcts.reform.blobrouter;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import uk.gov.hmcts.reform.blobrouter.data.envelopes.Status;

import java.util.concurrent.TimeUnit;

import static com.jayway.awaitility.Awaitility.await;
import static java.util.Arrays.asList;
import static uk.gov.hmcts.reform.blobrouter.envelope.ZipFileHelper.createZipArchive;
import static uk.gov.hmcts.reform.blobrouter.storage.StorageHelper.blobExists;
import static uk.gov.hmcts.reform.blobrouter.storage.StorageHelper.uploadFile;

public class CrimeBlobDispatchingTest extends FunctionalTestBase {

    @Override
    @BeforeEach
    protected void setUp() {
        super.setUp();
    }

    @Test
    @Disabled
    void should_move_extracted_crime_envelope_to_crime_storage() throws Exception {
        // upload crime file with unique name
        String fileName = randomFileName();

        byte[] wrappingZipContent = createZipArchive(
            asList("test-data/envelope/envelope.zip", "test-data/envelope/signature")
        );

        // when
        uploadFile(blobRouterStorageClient, config.crimeSourceContainer, fileName, wrappingZipContent);

        // then
        await("Wait for the blob to disappear from source container")
            .atMost(2, TimeUnit.MINUTES)
            .until(
                () -> !blobExists(blobRouterStorageClient, config.crimeSourceContainer, fileName)
            );

        assertFileInfoIsStored(fileName, config.crimeSourceContainer, Status.DISPATCHED, true);
    }
}
