package uk.gov.hmcts.reform.blobrouter.services.storage;

import com.azure.core.test.TestBase;
import com.azure.storage.blob.BlobContainerClient;
import com.azure.storage.blob.models.BlobStorageException;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.reform.blobrouter.util.StorageClientsHelper;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willThrow;
import static uk.gov.hmcts.reform.blobrouter.config.TargetStorageAccount.BULKSCAN;

@ExtendWith(MockitoExtension.class)
class BlobDispatcherTest extends TestBase {

    private static final String NEW_BLOB_NAME = "new.blob";
    private static final String DESTINATION_CONTAINER = "bulkscan";
    private static final String BOGUS_CONTAINER = "bogus";

    private BlobDispatcher dispatcher;

    @Mock
    BlobContainerClientProvider blobServiceClientProvider;

    @BeforeAll
    static void setUpTestMode() {
        StorageClientsHelper.setAzureTestMode();

        setupClass();
    }

    @Override
    protected void beforeTest() {
        dispatcher = new BlobDispatcher(blobServiceClientProvider);
    }

    @Test
    void should_upload_blob_to_dedicated_container() {
        BlobContainerClient containerClient =
            StorageClientsHelper.getContainerClient(interceptorManager, DESTINATION_CONTAINER);

        given(blobServiceClientProvider.get(any(), any())).willReturn(containerClient);

        assertThatCode(() -> dispatcher
            .dispatch(NEW_BLOB_NAME, NEW_BLOB_NAME.getBytes(), DESTINATION_CONTAINER, BULKSCAN)
        ).doesNotThrowAnyException();
    }

    @Test
    void should_rethrow_exceptions() {
        willThrow(new BlobStorageException("test exception", null, null))
            .given(blobServiceClientProvider)
            .get(any(), any());

        assertThatCode(() -> dispatcher
            .dispatch(NEW_BLOB_NAME, NEW_BLOB_NAME.getBytes(), BOGUS_CONTAINER, BULKSCAN)
        ).isInstanceOf(BlobStorageException.class);
    }
}
