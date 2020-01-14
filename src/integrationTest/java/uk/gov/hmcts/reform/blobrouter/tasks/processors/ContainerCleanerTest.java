package uk.gov.hmcts.reform.blobrouter.tasks.processors;

import com.azure.core.test.TestBase;
import com.azure.storage.blob.BlobServiceClient;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.testcontainers.containers.DockerComposeContainer;
import uk.gov.hmcts.reform.blobrouter.data.EnvelopeRepository;
import uk.gov.hmcts.reform.blobrouter.data.model.Envelope;
import uk.gov.hmcts.reform.blobrouter.util.StorageClientsHelper;

import java.time.Instant;
import java.util.UUID;

import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static uk.gov.hmcts.reform.blobrouter.data.model.Status.DISPATCHED;

@ExtendWith(MockitoExtension.class)
@ExtendWith(OutputCaptureExtension.class)
class ContainerCleanerTest extends TestBase {

    private static final String CONTAINER_WITH_BLOBS = "bulkscan";
    private static final String CONTAINER_WITHOUT_BLOBS = "empty";

    private ContainerCleaner containerCleaner;

    private EnvelopeRepository envelopeRepository;

    private static DockerComposeContainer dockerComposeContainer;

    @BeforeAll
    static void setUpTestMode() {
        StorageClientsHelper.setAzureTestMode();

        setupClass();
    }

    @Override
    protected void beforeTest() {
        BlobServiceClient storageClient = StorageClientsHelper.getStorageClient(interceptorManager);
        containerCleaner = new ContainerCleaner(storageClient, envelopeRepository);
    }

    @Test
    void should_find_blobs_and_process(CapturedOutput output) {
        // given
        UUID id1 = UUID.randomUUID();
        given(envelopeRepository.find(DISPATCHED, false)).willReturn(asList(
            getEnvelope(id1, "file1.zip")
        ));

        // when
        containerCleaner.process(CONTAINER_WITH_BLOBS);

        // then
        assertOutputCapture(output, CONTAINER_WITH_BLOBS);
    }

    @Test
    void should_not_find_any_blobs(CapturedOutput output) {
        // given
        given(envelopeRepository.find(DISPATCHED, false)).willReturn(emptyList());

        // when
        containerCleaner.process(CONTAINER_WITHOUT_BLOBS);

        // then
        assertOutputCapture(output, CONTAINER_WITHOUT_BLOBS);
    }

    private Envelope getEnvelope(UUID id, String fileName) {
        return new Envelope(
            id,
            CONTAINER_WITH_BLOBS,
            fileName,
            Instant.now(),
            Instant.now(),
            Instant.now(),
            DISPATCHED,
            false
        );
    }

    private void assertOutputCapture(CapturedOutput output, String containerName) {
        String simpleLoggerName = ContainerProcessor.class.getSimpleName();
        assertThat(output).contains(
            simpleLoggerName + " Started deleting dispatched blobs from container " + containerName
        );
        assertThat(output).contains(
            simpleLoggerName + " Finished deleting dispatched blobs from container " + containerName
        );
    }
}
