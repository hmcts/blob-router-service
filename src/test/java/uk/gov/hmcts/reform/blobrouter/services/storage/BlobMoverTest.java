package uk.gov.hmcts.reform.blobrouter.services.storage;

import com.azure.core.http.HttpResponse;
import com.azure.core.util.polling.PollResponse;
import com.azure.core.util.polling.SyncPoller;
import com.azure.storage.blob.BlobClient;
import com.azure.storage.blob.BlobContainerClient;
import com.azure.storage.blob.BlobServiceClient;
import com.azure.storage.blob.models.BlobCopyInfo;
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
import java.time.Duration;
import java.util.Base64;
import java.util.List;
import java.util.UUID;

import static com.google.common.io.Resources.getResource;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

@ExtendWith(MockitoExtension.class)
@SuppressWarnings("unchecked")
class BlobMoverTest {

    private BlobMover mover;

    @Mock
    BlobServiceClient storageClient;

    @Mock
    BlockBlobClient targetBlockBlobClient;

    private static final String CONTAINER_NAME = "testcontainer";
    private static final String REJECTED_CONTAINER_NAME = "testcontainer-rejected";
    private static final String BLOB_NAME = "testFile.zip";

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
        var contentStream = new ByteArrayInputStream(content);
        assertThrows(
            BlobStreamingException.class,
            () -> mover
                .uploadWithChunks(targetBlockBlobClient, contentStream)
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
        var contentStream = new ByteArrayInputStream(content);
        assertThrows(
            BlobStreamingException.class,
            () -> mover
                .uploadWithChunks(targetBlockBlobClient, contentStream)
        );

        verify(targetBlockBlobClient).delete();
    }

    @Test
    void should_skip_moving_if_blob_does_not_exist() {
        //given
        var sourceBlob = mockBlobClient(CONTAINER_NAME, BLOB_NAME);
        given(sourceBlob.exists()).willReturn(false);
        var targetBlob = mockBlobClient(REJECTED_CONTAINER_NAME, BLOB_NAME);
        // when
        mover.moveToRejectedContainer(BLOB_NAME, CONTAINER_NAME);
        // then
        verify(targetBlob).getContainerName();
        verify(sourceBlob).getContainerName();
        verify(sourceBlob).exists();
        verifyNoMoreInteractions(targetBlob);
        verifyNoMoreInteractions(sourceBlob);
    }

    @Test
    void should_move_to_rejected_and_delete_from_source_if_blob_exist() {
        // given
        var sourceBlob = mockBlobClient(CONTAINER_NAME, BLOB_NAME);
        var targetBlob = mockBlobClient(REJECTED_CONTAINER_NAME, BLOB_NAME);
        given(sourceBlob.exists()).willReturn(true);

        String sasToken = "sas_token_01-03-2021";
        given(sourceBlob.generateSas(any())).willReturn(sasToken);
        String blobUrl = "http://bloburl";
        given(sourceBlob.getBlobUrl()).willReturn(blobUrl);
        given(targetBlob.exists()).willReturn(true);

        SyncPoller syncPoller = mock(SyncPoller.class);
        given(targetBlob
            .beginCopy(
                eq(blobUrl + "?" + sasToken),
                eq(null),
                eq(null),
                eq(null),
                eq(null),
                eq(null),
                eq(Duration.ofSeconds(2))
            )
        ).willReturn(syncPoller);

        var pollResponse = mock(PollResponse.class);
        given(syncPoller.waitForCompletion(Duration.ofMinutes(5))).willReturn(pollResponse);
        given(pollResponse.getValue()).willReturn(mock(BlobCopyInfo.class));

        // when
        mover.moveToRejectedContainer(BLOB_NAME, CONTAINER_NAME);

        // then
        verify(sourceBlob).delete();
        verify(targetBlob).getContainerName();
        verify(targetBlob).exists();
        verify(targetBlob).createSnapshot();
        verifyNoMoreInteractions(targetBlob);
    }

    @Test
    void should_abort_copy_to_rejected_container_when_there_is_exception() {
        // given
        var sourceBlob = mockBlobClient(CONTAINER_NAME, BLOB_NAME);
        var targetBlob = mockBlobClient(REJECTED_CONTAINER_NAME, BLOB_NAME);
        given(sourceBlob.exists()).willReturn(true);

        given(sourceBlob.generateSas(any())).willReturn("sasToken");
        given(sourceBlob.getBlobUrl()).willReturn("blobUrl");

        SyncPoller syncPoller = mock(SyncPoller.class);
        given(targetBlob.beginCopy(any(), any(), any(), any(), any(), any(), any()))
            .willReturn(syncPoller);

        var pollResponse = mock(PollResponse.class);
        willThrow(new BlobStorageException("Copy Failed", mock(HttpResponse.class), null))
            .given(syncPoller).waitForCompletion(Duration.ofMinutes(5));

        var blobCopyInfo = mock(BlobCopyInfo.class);
        given(syncPoller.poll()).willReturn(pollResponse);
        given(pollResponse.getValue()).willReturn(blobCopyInfo);
        String copyId = UUID.randomUUID().toString();
        given(blobCopyInfo.getCopyId()).willReturn(copyId);

        // when
        assertThatThrownBy(
            () -> mover.moveToRejectedContainer(BLOB_NAME, CONTAINER_NAME)
        ).isInstanceOf(BlobStorageException.class);

        // then
        verify(sourceBlob, never()).delete();
        verify(targetBlob).abortCopyFromUrl(copyId);
    }

    @Test
    void should_omit_abort_copy_error_when_abortCopy_fails() {
        // given
        var sourceBlob = mockBlobClient(CONTAINER_NAME, BLOB_NAME);
        var targetBlob = mockBlobClient(REJECTED_CONTAINER_NAME, BLOB_NAME);
        given(sourceBlob.exists()).willReturn(true);

        given(sourceBlob.generateSas(any())).willReturn("sasToken");
        given(sourceBlob.getBlobUrl()).willReturn("blobUrl");

        SyncPoller syncPoller = mock(SyncPoller.class);
        given(targetBlob.beginCopy(any(), any(), any(), any(), any(), any(), any()))
            .willReturn(syncPoller);

        var pollResponse = mock(PollResponse.class);
        willThrow(new BlobStorageException("Copy Failed", mock(HttpResponse.class), null))
            .given(syncPoller).waitForCompletion(Duration.ofMinutes(5));

        willThrow(new RuntimeException("Polling failed"))
            .given(syncPoller).poll();

        // when
        assertThatThrownBy(
            () -> mover.moveToRejectedContainer(BLOB_NAME, CONTAINER_NAME)
        ).isInstanceOf(BlobStorageException.class);

        // then
        verify(sourceBlob, never()).delete();
        verify(targetBlob, never()).abortCopyFromUrl(any());
    }

    private BlockBlobClient mockBlobClient(String containerName, String blobName) {
        var blobContainerClient = mock(BlobContainerClient.class);
        given(storageClient.getBlobContainerClient(containerName)).willReturn(blobContainerClient);
        var blobClient = mock(BlobClient.class);
        given(blobContainerClient.getBlobClient(blobName)).willReturn(blobClient);
        var blockBlobClient = mock(BlockBlobClient.class);
        given(blobClient.getBlockBlobClient()).willReturn(blockBlobClient);
        return blockBlobClient;
    }
}