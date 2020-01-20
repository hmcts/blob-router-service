package uk.gov.hmcts.reform.blobrouter.services.storage;

import com.azure.core.test.TestBase;
import com.azure.storage.blob.BlobServiceClient;
import com.azure.storage.blob.models.BlobStorageException;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import uk.gov.hmcts.reform.blobrouter.util.StorageClientsHelper;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static uk.gov.hmcts.reform.blobrouter.config.TargetStorageAccount.BULKSCAN;

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
        BlobServiceClientProvider blobServiceClientProvider = mock(BlobServiceClientProvider.class);
        BlobServiceClient storageClient = StorageClientsHelper.getStorageClient(interceptorManager);
        given(blobServiceClientProvider.get(any(), any())).willReturn(storageClient);
        dispatcher = new BlobDispatcher(blobServiceClientProvider);
    }

    @Test
    void should_upload_blob_to_dedicated_container() {
        assertThatCode(() -> dispatcher
            .dispatch(NEW_BLOB_NAME, NEW_BLOB_NAME.getBytes(), DESTINATION_CONTAINER, BULKSCAN)
        ).doesNotThrowAnyException();
    }

    @Test
    void should_rethrow_exceptions() {
        assertThatCode(() -> dispatcher
            .dispatch(NEW_BLOB_NAME, NEW_BLOB_NAME.getBytes(), BOGUS_CONTAINER, BULKSCAN)
        ).isInstanceOf(BlobStorageException.class);
    }
}
