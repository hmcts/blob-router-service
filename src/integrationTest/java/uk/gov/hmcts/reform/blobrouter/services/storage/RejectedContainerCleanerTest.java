package uk.gov.hmcts.reform.blobrouter.services.storage;

import com.azure.storage.blob.BlobContainerClient;
import com.azure.storage.blob.models.BlobItem;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import uk.gov.hmcts.reform.blobrouter.data.EventRecordRepository;
import uk.gov.hmcts.reform.blobrouter.services.RejectedBlobChecker;
import uk.gov.hmcts.reform.blobrouter.tasks.processors.RejectedContainerCleaner;
import uk.gov.hmcts.reform.blobrouter.util.BlobStorageBaseTest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("db-test")
class RejectedContainerCleanerTest extends BlobStorageBaseTest {

    @Autowired EventRecordRepository eventRecordRepository;

    @Mock RejectedBlobChecker blobChecker;

    @Test
    void should_delete_files_from_rejected_container() {
        // given
        BlobContainerClient normalContainer = createContainer("sample-container");
        BlobContainerClient rejectedContainer = createContainer("sample-container-rejected");

        upload(normalContainer, "keep.zip");
        upload(rejectedContainer, "bye.zip");
        upload(rejectedContainer, "cya.zip");

        given(blobChecker.shouldBeDeleted(any())).willReturn(true); // always allow deleting blobs

        // when
        new RejectedContainerCleaner(storageClient, blobChecker, eventRecordRepository).cleanUp();

        // then
        assertThat(normalContainer.listBlobs())
            .extracting(BlobItem::getName)
            .as("File from input container should stay")
            .contains("keep.zip");

        assertThat(rejectedContainer.listBlobs())
            .as("Rejected files should be deleted")
            .isEmpty();
    }

    private void upload(BlobContainerClient containerClient, String fileName) {
        containerClient
            .getBlobClient(fileName)
            .uploadFromFile("src/integrationTest/resources/storage/test1.zip");
    }
}
