package uk.gov.hmcts.reform.blobrouter.services.storage;

import com.azure.core.test.TestBase;
import com.azure.storage.blob.BlobServiceAsyncClient;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import uk.gov.hmcts.reform.blobrouter.util.StorageClientsHelper;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThatCode;

class BlobDispatcherTest extends TestBase {

    private static final String NEW_BLOB_NAME = "new.blob";
    private static final String DESTINATION_CONTAINER = "bulkscan";

    private BlobDispatcher dispatcher;

    @BeforeAll
    static void setUpTestMode() {
        StorageClientsHelper.setAzureTestMode();

        setupClass();
    }

    @Override
    protected void beforeTest() {
        BlobServiceAsyncClient storageClient = StorageClientsHelper.getStorageClient(interceptorManager);
        dispatcher = new BlobDispatcher(storageClient);
    }

    @Test
    void should_upload_blob_to_dedicated_container() {
        assertThatCode(() -> dispatcher
            .dispatch(NEW_BLOB_NAME, NEW_BLOB_NAME.getBytes(), DESTINATION_CONTAINER)
            .block(Duration.ofSeconds(2)) // max waiting time
        ).doesNotThrowAnyException();
    }
}
