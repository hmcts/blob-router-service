package uk.gov.hmcts.reform.blobrouter.services.storage;

import com.azure.storage.blob.BlobClient;
import com.azure.storage.blob.BlobContainerClient;
import com.azure.storage.blob.BlobServiceClient;
import com.azure.storage.blob.specialized.BlockBlobClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.reform.blobrouter.data.EnvelopeRepository;
import uk.gov.hmcts.reform.blobrouter.data.model.Envelope;

import java.util.UUID;

import static java.time.Instant.now;
import static java.util.Arrays.asList;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static uk.gov.hmcts.reform.blobrouter.data.model.Status.REJECTED;

@ExtendWith(MockitoExtension.class)
class RejectedFilesHandlerTest {

    @Mock BlobServiceClient blobServiceClient;
    @Mock EnvelopeRepository repo;

    @Mock BlobContainerClient normalContainer1;
    @Mock BlobContainerClient normalContainer2;

    @Mock BlobContainerClient rejectedContainer1;
    @Mock BlobContainerClient rejectedContainer2;

    @Mock BlobClient normalBlob1;
    @Mock BlobClient normalBlob2;

    @Mock BlobClient rejectedBlob1;
    @Mock BlobClient rejectedBlob2;

    @Mock BlockBlobClient rejectedBlockBlob1;
    @Mock BlockBlobClient rejectedBlockBlob2;

    final Envelope envelope1 = new Envelope(UUID.randomUUID(), "c1", "f1", now(), now(), null, REJECTED, false);
    final Envelope envelope2 = new Envelope(UUID.randomUUID(), "c2", "f2", now(), now(), null, REJECTED, false);

    RejectedFilesHandler mover;

    @BeforeEach
    void setUp() {
        mover = new RejectedFilesHandler(blobServiceClient, repo);

        given(repo.find(REJECTED, false)).willReturn(asList(envelope1, envelope2));

        given(blobServiceClient.getBlobContainerClient("c1")).willReturn(normalContainer1);
        given(normalContainer1.getBlobClient("f1")).willReturn(normalBlob1);

        given(blobServiceClient.getBlobContainerClient("c2")).willReturn(normalContainer2);
        given(normalContainer2.getBlobClient("f2")).willReturn(normalBlob2);


        given(blobServiceClient.getBlobContainerClient("c1-rejected")).willReturn(rejectedContainer1);
        given(rejectedContainer1.getBlobClient("f1")).willReturn(rejectedBlob1);
        given(rejectedBlob1.getBlockBlobClient()).willReturn(rejectedBlockBlob1);

        given(blobServiceClient.getBlobContainerClient("c2-rejected")).willReturn(rejectedContainer2);
        given(rejectedContainer2.getBlobClient("f2")).willReturn(rejectedBlob2);
        given(rejectedBlob2.getBlockBlobClient()).willReturn(rejectedBlockBlob2);
    }

    @Test
    void should_handle_rejected_files() {
        // given
        // everything works

        // when
        mover.handle();

        // then
        verify(rejectedBlockBlob1).upload(any(), anyLong());
        verify(rejectedBlockBlob2).upload(any(), anyLong());

        verify(normalBlob1).delete();
        verify(normalBlob2).delete();

        verify(repo).markAsDeleted(envelope1.id);
        verify(repo).markAsDeleted(envelope2.id);
    }

    @Test
    void should_continue_moving_files_after_failure() {
        // given
        doThrow(RuntimeException.class)
            .when(normalBlob1)
            .delete();

        // when
        mover.handle();

        // then second files should get processed anyway...
        verify(rejectedBlockBlob2).upload(any(), anyLong());
        verify(normalBlob2).delete();
        verify(repo).markAsDeleted(envelope2.id);

        verify(repo, never()).markAsDeleted(envelope1.id);
    }
}
