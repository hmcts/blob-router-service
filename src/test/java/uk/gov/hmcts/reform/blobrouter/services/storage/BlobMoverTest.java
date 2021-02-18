package uk.gov.hmcts.reform.blobrouter.services.storage;

import com.azure.core.http.HttpResponse;
import com.azure.storage.blob.BlobServiceClient;
import com.azure.storage.blob.models.BlobStorageException;
import com.azure.storage.blob.models.BlockBlobItem;
import com.azure.storage.blob.specialized.BlockBlobClient;
import com.google.common.io.Resources;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.reform.blobrouter.exceptions.BlobStreamingException;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.Base64;
import java.util.List;

import static com.google.common.io.Resources.getResource;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
@SuppressWarnings("unchecked")
class BlobMoverTest {

    private BlobMover mover;

    @Mock
    BlobServiceClient storageClient;

    @Mock
    BlockBlobClient targetBlockBlobClient;

    @BeforeEach
    void setUp() {
        mover = new BlobMover(storageClient, 1024);
    }

    @Test
    void should_upload_in_chunks_when_it_uploads() throws IOException {
        // given

        //2830 bytes
        byte[] content = Resources.toByteArray(
            getResource("test1.pdf")
        );

        var expectedBlockIdList = List.of(
            Base64.getEncoder().encodeToString("0000001".getBytes()),
            Base64.getEncoder().encodeToString("0000002".getBytes()),
            Base64.getEncoder().encodeToString("0000003".getBytes())
        );
        doNothing().when(targetBlockBlobClient).stageBlock(anyString(), any(), anyLong());

        given(targetBlockBlobClient.commitBlockList(expectedBlockIdList))
            .willReturn(mock(BlockBlobItem.class));

        // when
        var blockList =
            mover.uploadWithChunks(targetBlockBlobClient, new ByteArrayInputStream(content));

        // then
        assertThat(blockList).containsExactlyElementsOf(expectedBlockIdList);
        var sizeCaptor = ArgumentCaptor.forClass(Long.class);

        verify(targetBlockBlobClient, times(3)).stageBlock(any(), any(), sizeCaptor.capture());
        assertThat(sizeCaptor.getAllValues())
            .containsExactlyElementsOf(List.of(1024L, 1024L, 782L));
        verify(targetBlockBlobClient, times(1)).commitBlockList(expectedBlockIdList);
    }

    @Test
    void should_try_to_delete_when_upload_with_chunks_get_error() throws IOException {

        //given
        var content = "test1234".getBytes();

        willThrow(new BlobStorageException("Stage upload failed", mock(HttpResponse.class), null))
            .given(targetBlockBlobClient)
            .stageBlock(anyString(), any(), anyLong());

        // when

        assertThrows(
            BlobStreamingException.class,
            () -> mover
                .uploadWithChunks(targetBlockBlobClient, new ByteArrayInputStream(content))
        );

        verify(targetBlockBlobClient).delete();
    }

    @Test
    void should_not_throw_exception_when_chunk_upload_delete_fails() throws IOException {

        //given
        var content = "test1234".getBytes();

        willThrow(new BlobStorageException("Stage upload failed", mock(HttpResponse.class), null))
            .given(targetBlockBlobClient)
            .stageBlock(anyString(), any(), anyLong());


        willThrow(new RuntimeException("Delete error"))
            .given(targetBlockBlobClient)
            .delete();

        // when

        assertThrows(
            BlobStreamingException.class,
            () -> mover
                .uploadWithChunks(targetBlockBlobClient, new ByteArrayInputStream(content))
        );

        verify(targetBlockBlobClient).delete();
    }
}