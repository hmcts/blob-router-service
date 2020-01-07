package uk.gov.hmcts.reform.blobrouter.tasks.processors;

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
class ContainerProcessorTest extends StorageTestBase {

    private static final String CONTAINER_WITH_BLOBS = "bulkscan";

    private ContainerProcessor containerProcessor;

    @Override
    protected void beforeTest() {
        containerProcessor = new ContainerProcessor();
    }

    @Test
    @SuppressWarnings("java:S2925") // ignore Thread.sleep() call
    void should_find_blobs_and_process(CapturedOutput output) throws InterruptedException {
        // when
        containerProcessor.process(StorageClientsHelper.getContainerClient(interceptorManager, CONTAINER_WITH_BLOBS));
        Thread.sleep(1000); // need to wait for subscriber to be notified

        // then
        assertOutputCapture(output, CONTAINER_WITH_BLOBS);
    }

    private void assertOutputCapture(CapturedOutput output, String containerName) {
        String simpleLoggerName = ContainerProcessor.class.getSimpleName();
        assertThat(output).contains(simpleLoggerName + " Processing container " + containerName);
    }
}
