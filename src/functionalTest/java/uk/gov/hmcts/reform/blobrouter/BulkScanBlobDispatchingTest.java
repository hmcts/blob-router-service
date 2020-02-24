package uk.gov.hmcts.reform.blobrouter;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.concurrent.TimeUnit;

import static com.jayway.awaitility.Awaitility.await;
import static java.util.Arrays.asList;
import static uk.gov.hmcts.reform.blobrouter.data.model.Status.DISPATCHED;
import static uk.gov.hmcts.reform.blobrouter.envelope.ZipFileHelper.createZipArchive;
import static uk.gov.hmcts.reform.blobrouter.storage.StorageHelper.blobExists;
import static uk.gov.hmcts.reform.blobrouter.storage.StorageHelper.uploadFile;

public class BulkScanBlobDispatchingTest extends FunctionalTestBase {

    @BeforeEach
    @Override
    protected void setUp() {
        super.setUp();
    }

    @Test
    void should_move_blob_to_bulkscan_storage() throws Exception {
        // upload bulkscan file with unique name
        String fileName = "Bulkscan_" + randomFileName();

        byte[] wrappingZipContent = createZipArchive(
            asList("test-data/envelope/envelope.zip", "test-data/envelope/signature")
        );

        // when
        uploadFile(blobRouterStorageClient, BULK_SCAN_CONTAINER, fileName, wrappingZipContent);

        // then
        await("Wait for the " + fileName + " to disappear from source container")
            .atMost(2, TimeUnit.MINUTES)
            .until(
                () -> !blobExists(blobRouterStorageClient, BULK_SCAN_CONTAINER, fileName)
            );

        // and
        assertFileInfoIsStored(fileName, BULK_SCAN_CONTAINER, DISPATCHED, true);
    }
}
