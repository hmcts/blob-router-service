package uk.gov.hmcts.reform.blobrouter.tasks.processors;

import com.azure.core.http.rest.PagedIterable;
import com.azure.storage.blob.BlobClient;
import com.azure.storage.blob.BlobContainerClient;
import com.azure.storage.blob.BlobServiceClient;
import com.azure.storage.blob.models.BlobContainerItem;
import com.azure.storage.blob.models.BlobItem;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.reform.blobrouter.data.events.EventType;
import uk.gov.hmcts.reform.blobrouter.services.EnvelopeService;
import uk.gov.hmcts.reform.blobrouter.services.RejectedBlobChecker;

import java.util.stream.Stream;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class RejectedContainerCleanerTest {

    private static final String REJECTED_CONTAINER = "abc-rejected";
    private static final String REJECTED_BLOB = "file2.zip";

    @Mock BlobServiceClient storageClient;
    @Mock RejectedBlobChecker blobChecker;
    @Mock EnvelopeService envelopeService;

    @Mock PagedIterable<BlobContainerItem> containers;
    @Mock BlobContainerItem container1Item;
    @Mock BlobContainerItem container2Item;

    @Mock BlobContainerClient container1Client;
    @Mock PagedIterable<BlobItem> container1BlobItems;

    @Mock(lenient = true) BlobContainerClient container2Client;
    @Mock PagedIterable<BlobItem> container2BlobItems;

    @Mock(lenient = true) BlobItem blobItem1;
    @Mock(lenient = true) BlobItem blobItem2;

    @Mock BlobClient blobClient1;
    @Mock BlobClient blobClient2;

    RejectedContainerCleaner cleaner;

    @BeforeEach
    void setUp() {
        this.cleaner = new RejectedContainerCleaner(storageClient, blobChecker, envelopeService);
    }

    @Test
    void should_clean_up_only_rejected_containers() {
        // given
        given(storageClient.listBlobContainers()).willReturn(containers);
        given(containers.stream()).willReturn(Stream.of(container1Item, container2Item));

        given(container1Item.getName()).willReturn("abc");
        given(container2Item.getName()).willReturn(REJECTED_CONTAINER);

        given(storageClient.getBlobContainerClient(REJECTED_CONTAINER)).willReturn(container2Client);
        given(container2Client.listBlobs(any(), any())).willReturn(container2BlobItems);

        // when
        cleaner.cleanUp();

        // then
        verify(storageClient, times(0)).getBlobContainerClient("abc");
        verify(storageClient, times(1)).getBlobContainerClient(REJECTED_CONTAINER);
    }

    @Test
    void should_skip_file_if_its_not_ready_to_be_deleted() {
        // given
        given(blobChecker.shouldBeDeleted(blobItem1)).willReturn(false);
        given(blobChecker.shouldBeDeleted(blobItem2)).willReturn(true);

        given(storageClient.listBlobContainers()).willReturn(containers);
        given(containers.stream()).willReturn(Stream.of(container1Item, container2Item));

        given(container1Item.getName()).willReturn("abc");
        given(container2Item.getName()).willReturn(REJECTED_CONTAINER);

        given(storageClient.getBlobContainerClient(REJECTED_CONTAINER)).willReturn(container2Client);
        given(container2Client.listBlobs(any(), any())).willReturn(container2BlobItems);

        given(container2BlobItems.stream()).willReturn(Stream.of(blobItem1, blobItem2));

        given(blobItem1.getName()).willReturn("file1.zip");
        given(blobItem2.getName()).willReturn(REJECTED_BLOB);

        given(container2Client.getBlobClient("file1.zip")).willReturn(blobClient1);
        given(container2Client.getBlobClient(REJECTED_BLOB)).willReturn(blobClient2);

        given(blobClient2.getContainerName()).willReturn(REJECTED_CONTAINER);
        given(blobClient2.getBlobName()).willReturn(REJECTED_BLOB);

        // when
        cleaner.cleanUp();

        // then
        verify(blobChecker).shouldBeDeleted(blobItem1);
        verify(blobChecker).shouldBeDeleted(blobItem2);

        verify(blobClient1, never()).delete();
        verify(blobClient2, times(1)).delete();

        // and
        verify(envelopeService).saveEvent(REJECTED_CONTAINER, REJECTED_BLOB, EventType.DELETED_FROM_REJECTED);
    }
}
