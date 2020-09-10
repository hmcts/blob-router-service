package uk.gov.hmcts.reform.blobrouter;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import uk.gov.hmcts.reform.blobrouter.data.envelopes.Status;

import java.util.concurrent.TimeUnit;

import static com.jayway.awaitility.Awaitility.await;
import static java.util.Arrays.asList;
import static uk.gov.hmcts.reform.blobrouter.envelope.ZipFileHelper.createZipArchive;
import static uk.gov.hmcts.reform.blobrouter.storage.StorageHelper.blobExists;
import static uk.gov.hmcts.reform.blobrouter.storage.StorageHelper.uploadFile;

public class PcqBlobDispatchingTest extends FunctionalTestBase {

    @Override
    @BeforeEach
    protected void setUp() {
        super.setUp();
    }

    @Test
    void should_move_extracted_pcq_envelope_to_pcq_storage() throws Exception {
        // upload crime file with unique name
        String fileName = randomFileName();

        byte[] wrappingZipContent = createZipArchive(
            asList("test-data/envelope/envelope.zip", "test-data/envelope/signature")
        );

        // when
        uploadFile(blobRouterStorageClient, config.pcqSourceContainer, fileName, wrappingZipContent);

        // then
        await("Wait for the blob to disappear from source container")
            .atMost(2, TimeUnit.MINUTES)
            .until(
                () -> !blobExists(blobRouterStorageClient, config.pcqSourceContainer, fileName)
            );

        assertFileInfoIsStored(fileName, config.pcqSourceContainer, Status.DISPATCHED, true);
    }
}
