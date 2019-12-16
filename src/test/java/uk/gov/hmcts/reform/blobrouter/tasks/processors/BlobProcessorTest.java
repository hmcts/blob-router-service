package uk.gov.hmcts.reform.blobrouter.tasks.processors;

import com.azure.storage.blob.BlobAsyncClient;
import com.azure.storage.blob.models.BlobItem;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import uk.gov.hmcts.reform.blobrouter.util.StorageClientsHelper;
import uk.gov.hmcts.reform.blobrouter.util.StorageTestBase;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(OutputCaptureExtension.class)
class BlobProcessorTest extends StorageTestBase {

    private static final String CONTAINER_NAME = "bulkscan";

    private static final BlobProcessor PROCESSOR = new BlobProcessor();

    @DisplayName("Process single random blob")
    @Test
    void should_process_single_blob(CapturedOutput output) throws InterruptedException {
        // given
        BlobAsyncClient blobClient = StorageClientsHelper.getBlobClient(
            interceptorManager,
            CONTAINER_NAME,
            testResourceNamer.randomUuid()
        );
        BlobItem blob = new BlobItem().setName(interceptorManager.getRecordedData().removeVariable());

        // when
        PROCESSOR.process(blobClient, StorageClientsHelper.getLeaseClient(blobClient), blob);
        Thread.sleep(1000); // need to wait for subscriber to be notified

        // then
        assertThat(output).contains("Processing blob " + blob.getName() + " from container " + CONTAINER_NAME);
    }
}
