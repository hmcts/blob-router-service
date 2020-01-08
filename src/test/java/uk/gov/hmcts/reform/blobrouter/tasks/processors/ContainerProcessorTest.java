package uk.gov.hmcts.reform.blobrouter.tasks.processors;

import com.azure.storage.blob.BlobServiceAsyncClient;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import uk.gov.hmcts.reform.blobrouter.util.StorageClientsHelper;
import uk.gov.hmcts.reform.blobrouter.util.StorageTestBase;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
@ExtendWith(OutputCaptureExtension.class)
@SuppressWarnings("java:S2925") // ignore Thread.sleep() call in the tests
class ContainerProcessorTest extends StorageTestBase {

    private static final String CONTAINER_WITH_BLOBS = "bulkscan";
    private static final String CONTAINER_WITHOUT_BLOBS = "empty";

    private ContainerProcessor containerProcessor;

    private BlobServiceAsyncClient storageClient;

    @Override
    protected void beforeTest() {
        storageClient = StorageClientsHelper.getStorageClient(interceptorManager);
        containerProcessor = new ContainerProcessor(storageClient);
    }

    @Test
    void should_find_blobs_and_process(CapturedOutput output) throws InterruptedException {
        // when
        containerProcessor.process(CONTAINER_WITH_BLOBS);
        Thread.sleep(1000); // need to wait for subscriber to be notified

        // then
        assertOutputCapture(output, CONTAINER_WITH_BLOBS);
    }

    @Test
    void should_not_find_any_blobs(CapturedOutput output) throws InterruptedException {
        // when
        containerProcessor.process(CONTAINER_WITHOUT_BLOBS);
        Thread.sleep(1000); // need to wait for subscriber to be notified

        // then
        assertOutputCapture(output, CONTAINER_WITHOUT_BLOBS);
    }

    private void assertOutputCapture(CapturedOutput output, String containerName) {
        String simpleLoggerName = ContainerProcessor.class.getSimpleName();
        assertThat(output).contains(simpleLoggerName + " Processing container " + containerName);
        assertThat(output).contains(
            simpleLoggerName + " Finished processing container " + containerName
        );
    }
}
