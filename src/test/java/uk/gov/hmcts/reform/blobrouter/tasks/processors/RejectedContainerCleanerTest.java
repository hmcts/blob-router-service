package uk.gov.hmcts.reform.blobrouter.tasks.processors;

import com.azure.storage.blob.BlobServiceClient;
import org.junit.jupiter.api.Test;
import org.mockito.BDDMockito;
import org.mockito.Mock;
import uk.gov.hmcts.reform.blobrouter.services.RejectedBlobChecker;

import static org.mockito.BDDMockito.given;

class RejectedContainerCleanerTest {

    @Mock BlobServiceClient storageClient;
    @Mock RejectedBlobChecker blobChecker;

    @Test
    void should_clean_up_only_rejected_containers() {
        // given
        var cleaner = new RejectedContainerCleaner(storageClient, blobChecker);

        given(storageClient.listBlobContainers()).

        // when
        //cleaner.cleanUp();

        // then
    }
}
