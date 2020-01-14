package uk.gov.hmcts.reform.blobrouter.services.storage;

import com.azure.core.test.TestBase;
import com.azure.storage.blob.BlobServiceClient;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import uk.gov.hmcts.reform.blobrouter.util.StorageClientsHelper;

import static org.assertj.core.api.Assertions.assertThatCode;

class BlobDispatcherTest extends TestBase {

    private static final String NEW_BLOB_NAME = "new.blob";
    private static final String DESTINATION_CONTAINER = "bulkscan";
    private static final String BOGUS_CONTAINER = "bogus";

    private BlobDispatcher dispatcher;

    @BeforeAll
    static void setUpTestMode() {
        StorageClientsHelper.setAzureTestMode();

        setupClass();
    }

    @Override
    protected void beforeTest() {
        BlobServiceClient storageClient = StorageClientsHelper.getStorageClient(interceptorManager);
        dispatcher = new BlobDispatcher(storageClient);
    }

    @Test
    void should_upload_blob_to_dedicated_container() {
        assertThatCode(() -> dispatcher
            .dispatch(NEW_BLOB_NAME, NEW_BLOB_NAME.getBytes(), DESTINATION_CONTAINER)
        ).doesNotThrowAnyException();
    }

    @Test
    void should_catch_BlobStorageException_and_suppress_it() {
        assertThatCode(() -> dispatcher
            .dispatch(NEW_BLOB_NAME, NEW_BLOB_NAME.getBytes(), BOGUS_CONTAINER)
        ).doesNotThrowAnyException();
    }
}
