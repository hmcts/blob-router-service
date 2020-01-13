package uk.gov.hmcts.reform.blobrouter.tasks.processors;

import com.azure.core.test.TestBase;
import com.azure.storage.blob.BlobServiceClient;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.reform.blobrouter.util.StorageClientsHelper;

import static org.assertj.core.api.Assertions.assertThatCode;

@ExtendWith(MockitoExtension.class)
class ContainerProcessorTest extends TestBase {

    private static final String CONTAINER_WITH_BLOBS = "bulkscan";
    private static final String CONTAINER_WITHOUT_BLOBS = "empty";

    private ContainerProcessor containerProcessor;

    @BeforeAll
    static void setUpTestMode() {
        StorageClientsHelper.setAzureTestMode();

        setupClass();
    }

    @Override
    protected void beforeTest() {
        BlobServiceClient storageClient = StorageClientsHelper.getStorageClient(interceptorManager);
        containerProcessor = new ContainerProcessor(storageClient);
    }

    @Test
    void should_find_blobs_and_process() {
        assertThatCode(() -> containerProcessor.process(CONTAINER_WITH_BLOBS))
            .doesNotThrowAnyException();
    }

    @Test
    void should_not_find_any_blobs() {
        assertThatCode(() -> containerProcessor.process(CONTAINER_WITHOUT_BLOBS))
            .doesNotThrowAnyException();
    }
}
