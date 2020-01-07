package uk.gov.hmcts.reform.blobrouter.tasks.processors;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import uk.gov.hmcts.reform.blobrouter.util.StorageClientsHelper;
import uk.gov.hmcts.reform.blobrouter.util.StorageTestBase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
@ExtendWith(OutputCaptureExtension.class)
class ContainerProcessorTest extends StorageTestBase {

    private static final String CONTAINER_WITH_BLOBS = "bulkscan";
    private static final String CONTAINER_WITHOUT_BLOBS = "empty";

    @Mock
    private BlobProcessor blobProcessor;

    private ContainerProcessor containerProcessor;

    @Override
    protected void beforeTest() {
        containerProcessor = new ContainerProcessor(blobProcessor);
    }

    @DisplayName("Find 1 blob and pass it to BlobProcessor")
    @Test
    void should_find_one_blob_and_pass_it_to_blob_processor(CapturedOutput output) throws InterruptedException {
        // when
        containerProcessor.process(StorageClientsHelper.getContainerClient(interceptorManager, CONTAINER_WITH_BLOBS));
        Thread.sleep(1000); // need to wait for subscriber to be notified

        // then
        //verify(blobProcessor, times(1)).process(any(), any(), any());

        // and
        assertOutputCapture(output, CONTAINER_WITH_BLOBS, 1);
    }

    @DisplayName("No blobs found will not call BlobProcessor")
    @Test
    void should_not_find_any_blobs(CapturedOutput output) throws InterruptedException {
        // when
        containerProcessor.process(
            StorageClientsHelper.getContainerClient(interceptorManager, CONTAINER_WITHOUT_BLOBS)
        );
        Thread.sleep(1000); // need to wait for subscriber to be notified

        // then
        verify(blobProcessor, never()).process(any(), any(), any());

        // and
        assertOutputCapture(output, CONTAINER_WITHOUT_BLOBS, 0);
    }

    private void assertOutputCapture(CapturedOutput output, String containerName, int blobCount) {
        String simpleLoggerName = ContainerProcessor.class.getSimpleName();
        assertThat(output).contains(simpleLoggerName + " Processing container " + containerName);
        assertThat(output).contains(
            simpleLoggerName + " Finished processing container " + containerName + ". Blobs processed: " + blobCount
        );
    }
}
