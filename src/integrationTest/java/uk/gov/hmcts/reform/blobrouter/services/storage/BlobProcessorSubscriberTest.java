package uk.gov.hmcts.reform.blobrouter.services.storage;

import com.azure.core.test.TestBase;
import com.azure.storage.blob.BlobAsyncClient;
import com.azure.storage.blob.BlobServiceAsyncClient;
import com.azure.storage.blob.specialized.BlobLeaseAsyncClient;
import com.azure.storage.blob.specialized.BlobLeaseClientBuilder;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import reactor.util.function.Tuples;
import uk.gov.hmcts.reform.blobrouter.util.StorageClientsHelper;

import java.nio.ByteBuffer;

import static org.assertj.core.api.Assertions.assertThatCode;

public class BlobProcessorSubscriberTest extends TestBase {

    private static final String BLOB_NAME = "new.blob";
    private static final String CONTAINER = "bulkscan";

    private BlobProcessorSubscriber subscriber;

    @BeforeAll
    static void setUpTestMode() {
        StorageClientsHelper.setAzureTestMode();

        setupClass();
    }

    @Override
    protected void beforeTest() {
        BlobServiceAsyncClient storageClient = StorageClientsHelper.getStorageClient(interceptorManager);
        BlobAsyncClient blobClient = storageClient
            .getBlobContainerAsyncClient(CONTAINER)
            .getBlobAsyncClient(BLOB_NAME);
        BlobLeaseAsyncClient blobLeaseClient = new BlobLeaseClientBuilder()
            .blobAsyncClient(blobClient)
            .buildAsyncClient();
        BlobDispatcher dispatcher = new BlobDispatcher(storageClient);

        subscriber = new BlobProcessorSubscriber(blobClient, blobLeaseClient, dispatcher);
    }

    @Test
    void should_process_single_blob() {
        assertThatCode(() -> subscriber.onNext(Tuples.of(ByteBuffer.wrap(BLOB_NAME.getBytes()), "lease ID")))
            .doesNotThrowAnyException();
    }

    @Test
    void should_log_error_and_not_throw_exception_when_process_receives_something_unexpected() {
        assertThatCode(() -> subscriber.onError(new RuntimeException("oh no"))).doesNotThrowAnyException();
    }
}
