package uk.gov.hmcts.reform.blobrouter.services.storage;

import com.azure.core.test.TestBase;
import com.azure.storage.blob.BlobServiceAsyncClient;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import uk.gov.hmcts.reform.blobrouter.util.StorageClientsHelper;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.zip.ZipInputStream;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(OutputCaptureExtension.class)
@SuppressWarnings("java:S2925") // ignore Thread.sleep() call in the tests
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
    void should_upload_blob_to_dedicated_container(CapturedOutput output) throws IOException, InterruptedException {
        try (ZipInputStream inputStream = new ZipInputStream(new ByteArrayInputStream(NEW_BLOB_NAME.getBytes()))) {
            dispatcher.dispatch(NEW_BLOB_NAME, inputStream, DESTINATION_CONTAINER);
        }

        Thread.sleep(1000); // need to wait for subscriber to be notified

        assertThat(output).contains(
            "Finished uploading " + NEW_BLOB_NAME + " to " + DESTINATION_CONTAINER + " container"
        );
    }
}
