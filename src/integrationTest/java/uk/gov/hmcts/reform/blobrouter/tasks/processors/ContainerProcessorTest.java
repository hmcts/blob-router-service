package uk.gov.hmcts.reform.blobrouter.tasks.processors;

import com.azure.storage.blob.BlobClient;
import com.azure.storage.blob.BlobContainerClient;
import com.azure.storage.blob.specialized.BlobClientBase;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import uk.gov.hmcts.reform.blobrouter.data.DbHelper;
import uk.gov.hmcts.reform.blobrouter.services.BlobReadinessChecker;
import uk.gov.hmcts.reform.blobrouter.services.EnvelopeService;
import uk.gov.hmcts.reform.blobrouter.util.BlobStorageBaseTest;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@SpringBootTest
@ExtendWith(MockitoExtension.class)
@ActiveProfiles("db-test")
class ContainerProcessorTest extends BlobStorageBaseTest {

    private static final String CONTAINER_NAME = "my-container";

    @Autowired EnvelopeService envelopeService;
    @Autowired DbHelper dbHelper;

    @Mock BlobProcessor blobProcessor;
    @Mock BlobReadinessChecker blobReadinessChecker;

    private ContainerProcessor containerProcessor;
    private BlobContainerClient containerClient;

    @BeforeEach
    void setUp() {
        containerProcessor = new ContainerProcessor(
            storageClient,
            blobProcessor,
            blobReadinessChecker,
            envelopeService
        );
        containerClient = createContainer(CONTAINER_NAME);
    }

    @AfterEach
    void tearDown() {
        dbHelper.deleteAll();
        deleteAllContainers();
    }

    @Test
    void should_read_files_from_provided_container_and_call_blob_processor_for_each_ready_blob() {
        // given
        upload(containerClient, "1.zip");
        upload(containerClient, "2.zip");
        upload(containerClient, "3.zip");

        given(blobReadinessChecker.isReady(any())).willReturn(true, false, true);

        // when
        containerProcessor.process(CONTAINER_NAME);

        // then
        var blobArgCaptor = ArgumentCaptor.forClass(BlobClient.class);
        verify(blobProcessor, times(2)).process(blobArgCaptor.capture());

        assertThat(blobArgCaptor.getAllValues())
            .extracting(BlobClientBase::getBlobName)
            .containsExactly("1.zip", "3.zip");
    }

    @Test
    void should_not_call_blob_processor_when_file_already_exists_in_database() {
        // given
        upload(containerClient, "4.zip");
        envelopeService.createDispatchedEnvelope(CONTAINER_NAME, "4.zip", Instant.now());
        given(blobReadinessChecker.isReady(any())).willReturn(true);

        // when
        containerProcessor.process(CONTAINER_NAME);

        // then
        verify(blobProcessor, never()).process(any());
    }

    void upload(BlobContainerClient containerClient, String fileName) {
        containerClient
            .getBlobClient(fileName)
            .uploadFromFile("src/integrationTest/resources/storage/test1.zip");
    }
}
