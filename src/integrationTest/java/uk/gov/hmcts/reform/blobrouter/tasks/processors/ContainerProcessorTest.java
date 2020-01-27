package uk.gov.hmcts.reform.blobrouter.tasks.processors;

import com.azure.storage.blob.BlobContainerClient;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import uk.gov.hmcts.reform.blobrouter.util.BlobStorageBaseTest;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

@ActiveProfiles("db-test")
@SpringBootTest
@ExtendWith(MockitoExtension.class)
class ContainerProcessorTest extends BlobStorageBaseTest {

    @Mock BlobProcessor blobProcessor;

    @Test
    void should_read_files_from_provided_container() {
        // given
        var containerName = "my-container";

        var containerClient = createContainer(containerName);

        upload(containerClient, "1.zip");
        upload(containerClient, "2.zip");

        var containerProcessor = new ContainerProcessor(storageClient, blobProcessor);

        // when
        containerProcessor.process(containerName);

        // then
        verify(blobProcessor).process("1.zip", containerName);
        verify(blobProcessor).process("2.zip", containerName);
        verifyNoMoreInteractions(blobProcessor);
    }

    void upload(BlobContainerClient containerClient, String fileName) {
        containerClient
            .getBlobClient(fileName)
            .uploadFromFile("src/integrationTest/resources/storage/test1.zip");
    }
}
