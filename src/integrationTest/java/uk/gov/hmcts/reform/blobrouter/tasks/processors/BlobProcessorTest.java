package uk.gov.hmcts.reform.blobrouter.tasks.processors;

import com.azure.core.test.TestBase;
import com.azure.storage.blob.BlobServiceClient;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import uk.gov.hmcts.reform.blobrouter.services.storage.BlobDispatcher;
import uk.gov.hmcts.reform.blobrouter.util.StorageClientsHelper;

import static org.assertj.core.api.Assertions.assertThatCode;

class BlobProcessorTest extends TestBase {

    private static final String NEW_BLOB_NAME = "new.blob";
    private static final String CONTAINER = "bulkscan";
    private static final String BOGUS_CONTAINER = "bogus";

    private BlobProcessor blobProcessor;

    @BeforeAll
    static void setUpTestMode() {
        StorageClientsHelper.setAzureTestMode();

        setupClass();
    }

    @Override
    protected void beforeTest() {
        BlobServiceClient storageClient = StorageClientsHelper.getStorageClient(interceptorManager);
        BlobDispatcher dispatcher = new BlobDispatcher(storageClient);
        blobProcessor = new BlobProcessor(storageClient, dispatcher);
    }

    @Test
    void should_find_blobs_and_process() {
        assertThatCode(() -> blobProcessor.process(NEW_BLOB_NAME, CONTAINER))
            .doesNotThrowAnyException();
    }

    @Test
    void should_catch_BlobStorageException_and_suppress_it() {
        assertThatCode(() -> blobProcessor.process(NEW_BLOB_NAME, BOGUS_CONTAINER))
            .doesNotThrowAnyException();
    }
}
