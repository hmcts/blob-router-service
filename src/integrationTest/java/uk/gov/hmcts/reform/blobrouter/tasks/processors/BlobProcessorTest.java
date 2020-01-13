package uk.gov.hmcts.reform.blobrouter.tasks.processors;

import com.azure.core.test.TestBase;
import com.azure.storage.blob.BlobServiceAsyncClient;
import com.azure.storage.blob.models.BlobItem;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import uk.gov.hmcts.reform.blobrouter.services.storage.BlobDispatcher;
import uk.gov.hmcts.reform.blobrouter.util.StorageClientsHelper;

class BlobProcessorTest extends TestBase {

    private static final String NEW_BLOB_NAME = "new.blob";
    private static final String CONTAINER = "bulkscan";

    private BlobProcessor processor;

    @BeforeAll
    static void setUpTestMode() {
        StorageClientsHelper.setAzureTestMode();

        setupClass();
    }

    @Override
    protected void beforeTest() {
        BlobServiceAsyncClient storageClient = StorageClientsHelper.getStorageClient(interceptorManager);
        BlobDispatcher dispatcher = new BlobDispatcher(storageClient);

        processor = new BlobProcessor(storageClient, dispatcher);
    }

    @Test
    void should() {
        BlobItem blob = new BlobItem();
        blob.setName(NEW_BLOB_NAME);

        processor.process(blob, CONTAINER);
    }
}
