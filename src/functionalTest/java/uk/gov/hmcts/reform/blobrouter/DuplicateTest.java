package uk.gov.hmcts.reform.blobrouter;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import java.util.concurrent.TimeUnit;

import static com.jayway.awaitility.Awaitility.await;
import static java.util.Arrays.asList;
import static uk.gov.hmcts.reform.blobrouter.envelope.ZipFileHelper.createZipArchive;
import static uk.gov.hmcts.reform.blobrouter.storage.StorageHelper.blobExists;
import static uk.gov.hmcts.reform.blobrouter.storage.StorageHelper.uploadFile;

@Disabled
public class DuplicateTest extends FunctionalTestBase {

    @Override
    @BeforeEach
    protected void setUp() {
        super.setUp();
    }

    @Test
    void should_reject_duplicate_envelope() throws Exception {
        // given
        final String fileName = randomFileName();

        byte[] content =
            createZipArchive(
                asList(
                    "test-data/envelope/envelope.zip",
                    "test-data/envelope/invalid-signature"
                )
            );

        // and
        uploadFile(blobRouterStorageClient, BULK_SCAN_CONTAINER, fileName, content);
        await("Wait for the blob to disappear from source container")
            .atMost(2, TimeUnit.MINUTES)
            .until(() -> !blobExists(blobRouterStorageClient, BULK_SCAN_CONTAINER, fileName));

        // when
        // upload same file again
        uploadFile(blobRouterStorageClient, BULK_SCAN_CONTAINER, fileName, content);

        // then
        await("Wait for the blob to disappear from source container")
            .atMost(2, TimeUnit.MINUTES)
            .until(() -> !blobExists(blobRouterStorageClient, BULK_SCAN_CONTAINER, fileName));
    }
}
