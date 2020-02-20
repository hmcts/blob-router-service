package uk.gov.hmcts.reform.blobrouter.tasks.processors;

import com.azure.storage.blob.BlobContainerClient;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import uk.gov.hmcts.reform.blobrouter.services.BlobReadinessChecker;
import uk.gov.hmcts.reform.blobrouter.services.EnvelopeService;
import uk.gov.hmcts.reform.blobrouter.util.BlobStorageBaseTest;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

@SpringBootTest
@ExtendWith(MockitoExtension.class)
class ContainerProcessorTest extends BlobStorageBaseTest {

    @Autowired EnvelopeService envelopeService;

    @Mock BlobProcessor blobProcessor;
    @Mock BlobReadinessChecker blobReadinessChecker;

    @Test
    void should_read_files_from_provided_container_and_call_blob_processor_for_each_ready_blob() {
        // given
        var containerName = "my-container";

        var containerClient = createContainer(containerName);

        upload(containerClient, "1.zip");
        upload(containerClient, "2.zip");
        upload(containerClient, "3.zip");

        given(blobReadinessChecker.isReady(any())).willReturn(true, false, true);

        var containerProcessor = new ContainerProcessor(
            storageClient,
            blobProcessor,
            blobReadinessChecker,
            envelopeService
        );

        // when
        containerProcessor.process(containerName);

        // then
        verify(blobProcessor).process("1.zip", containerName);
        verify(blobProcessor).process("3.zip", containerName);
        verifyNoMoreInteractions(blobProcessor);
    }

    void upload(BlobContainerClient containerClient, String fileName) {
        containerClient
            .getBlobClient(fileName)
            .uploadFromFile("src/integrationTest/resources/storage/test1.zip");
    }
}
