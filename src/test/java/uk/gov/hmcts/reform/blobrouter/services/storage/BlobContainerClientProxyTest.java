package uk.gov.hmcts.reform.blobrouter.services.storage;

import com.azure.core.http.HttpResponse;
import com.azure.core.util.polling.PollResponse;
import com.azure.core.util.polling.SyncPoller;
import com.azure.storage.blob.BlobClient;
import com.azure.storage.blob.BlobContainerClient;
import com.azure.storage.blob.BlobContainerClientBuilder;
import com.azure.storage.blob.models.BlobCopyInfo;
import com.azure.storage.blob.models.BlobStorageException;
import com.azure.storage.blob.models.BlockBlobItem;
import com.azure.storage.blob.specialized.BlobInputStream;
import com.azure.storage.blob.specialized.BlockBlobClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.AdditionalAnswers;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.reform.blobrouter.config.TargetStorageAccount;
import uk.gov.hmcts.reform.blobrouter.exceptions.InvalidZipArchiveException;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.Duration;
import java.util.Map;
import java.util.UUID;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static uk.gov.hmcts.reform.blobrouter.services.storage.BlobContainerClientProxy.META_DATA_MAP;
import static uk.gov.hmcts.reform.blobrouter.util.zipverification.ZipVerifiers.ENVELOPE;
import static uk.gov.hmcts.reform.blobrouter.util.zipverification.ZipVerifiers.SIGNATURE;

@SuppressWarnings("unchecked")
@ExtendWith(MockitoExtension.class)
public class BlobContainerClientProxyTest {

    @Mock BlobContainerClient crimeClient;
    @Mock SasTokenCache sasTokenCache;
    @Mock BlobContainerClientBuilder blobContainerClientBuilder;
    @Mock BlobContainerClientBuilderProvider blobContainerClientBuilderProvider;

    BlobContainerClientProxy blobContainerClientProxy;

    @Mock BlobContainerClient blobContainerClient;
    @Mock BlobClient blobClient;
    @Mock BlobClient sourceBlobClient;
    @Mock BlockBlobClient sourceBlockBlobClient;

    @Mock BlockBlobClient blockBlobClient;

    final String containerName = "container123";
    final String blobName = "hello.zip";
    final byte[] blobContent = "some data".getBytes();

    @BeforeEach
    private void setUp() {
        this.blobContainerClientProxy = new BlobContainerClientProxy(
            crimeClient,
            blobContainerClientBuilderProvider,
            sasTokenCache
        );
    }

    @Test
    void should_upload_to_crime_storage_when_target_storage_crime() {
        given(crimeClient.getBlobClient(blobName)).willReturn(blobClient);
        given(blobClient.getBlockBlobClient()).willReturn(blockBlobClient);

        blobContainerClientProxy.upload(
            blobName,
            blobContent,
            containerName,
            TargetStorageAccount.CRIME
        );

        // then
        ArgumentCaptor<ByteArrayInputStream> data = ArgumentCaptor.forClass(ByteArrayInputStream.class);

        verify(blockBlobClient)
            .upload(
                data.capture(),
                eq(Long.valueOf(blobContent.length))
            );

        assertThat(data.getValue().readAllBytes()).isEqualTo(blobContent);

        verify(sasTokenCache, never()).getSasToken(containerName);
    }

    @Test
    void should_upload_to_bulk_scan_storage_when_target_storage_bulk_scan() {

        given(sasTokenCache.getSasToken(any())).willReturn("token1");

        given(blobContainerClientBuilderProvider.getBlobContainerClientBuilder())
            .willReturn(blobContainerClientBuilder);
        given(blobContainerClientBuilder.containerName(containerName)).willReturn(blobContainerClientBuilder);
        given(blobContainerClientBuilder.sasToken("token1")).willReturn(blobContainerClientBuilder);
        given(blobContainerClientBuilder.buildClient()).willReturn(blobContainerClient);

        given(blobContainerClient.getBlobClient(blobName)).willReturn(blobClient);
        given(blobClient.getBlockBlobClient()).willReturn(blockBlobClient);


        blobContainerClientProxy.upload(
            blobName,
            blobContent,
            containerName,
            TargetStorageAccount.CFT
        );

        verify(sasTokenCache).getSasToken(containerName);

        // then
        ArgumentCaptor<ByteArrayInputStream> data = ArgumentCaptor.forClass(ByteArrayInputStream.class);

        verify(blockBlobClient)
            .upload(
                data.capture(),
                eq(Long.valueOf(blobContent.length))
            );

        assertThat(data.getValue().readAllBytes()).isEqualTo(blobContent);

        verify(sasTokenCache, never()).removeFromCache(containerName);

    }

    @ParameterizedTest
    @EnumSource(
        value = TargetStorageAccount.class,
        names = {"CFT", "PCQ"}
    )
    void should_invalidate_cache_when_upload_returns_error_response_40x(TargetStorageAccount storageAccount) {
        // given
        HttpResponse mockHttpResponse = mock(HttpResponse.class);
        given(mockHttpResponse.getStatusCode()).willReturn(401);

        if (storageAccount == TargetStorageAccount.PCQ) {
            given(blobContainerClientBuilderProvider.getPcqBlobContainerClientBuilder())
                .willReturn(blobContainerClientBuilder);
        } else if (storageAccount == TargetStorageAccount.CFT) {
            given(blobContainerClientBuilderProvider.getBlobContainerClientBuilder())
                .willReturn(blobContainerClientBuilder);
        }

        willThrow(new BlobStorageException("Sas invalid 401", mockHttpResponse, null))
            .given(blobContainerClientBuilder)
            .sasToken(any());

        // then
        assertThatThrownBy(
            () -> blobContainerClientProxy.upload(
                blobName,
                blobContent,
                containerName,
                storageAccount
            )
        ).isInstanceOf(BlobStorageException.class);

        verify(sasTokenCache).removeFromCache(containerName);

    }

    @Test
    void should_not_invalidate_cache_when_target_storage_crime_and_error_response_40x() {

        HttpResponse mockHttpResponse = mock(HttpResponse.class);
        willThrow(new BlobStorageException("Sas invalid 401", mockHttpResponse, null))
            .given(crimeClient)
            .getBlobClient(any());
        assertThatThrownBy(
            () -> blobContainerClientProxy.upload(
                blobName,
                blobContent,
                containerName,
                TargetStorageAccount.CRIME
            )
        ).isInstanceOf(BlobStorageException.class);

        verify(sasTokenCache, never()).removeFromCache(containerName);

    }

    @Test
    void should_upload_to_pcq_storage_when_target_storage_is_pcq() {
        // given
        given(sasTokenCache.getPcqSasToken(any())).willReturn("token1");

        given(blobContainerClientBuilderProvider.getPcqBlobContainerClientBuilder())
            .willReturn(blobContainerClientBuilder);
        given(blobContainerClientBuilder.containerName(containerName)).willReturn(blobContainerClientBuilder);
        given(blobContainerClientBuilder.sasToken("token1")).willReturn(blobContainerClientBuilder);
        given(blobContainerClientBuilder.buildClient()).willReturn(blobContainerClient);

        given(blobContainerClient.getBlobClient(blobName)).willReturn(blobClient);
        given(blobClient.getBlockBlobClient()).willReturn(blockBlobClient);

        // when
        blobContainerClientProxy.upload(
            blobName,
            blobContent,
            containerName,
            TargetStorageAccount.PCQ
        );

        verify(sasTokenCache).getPcqSasToken(containerName);

        // then
        ArgumentCaptor<ByteArrayInputStream> data = ArgumentCaptor.forClass(ByteArrayInputStream.class);

        verify(blockBlobClient)
            .upload(
                data.capture(),
                eq(Long.valueOf(blobContent.length))
            );

        assertThat(data.getValue().readAllBytes()).isEqualTo(blobContent);
        verify(sasTokenCache, never()).removeFromCache(containerName);
    }

    @Test
    void should_move_to_bulk_scan_storage_when_target_storage_bulk_scan() {

        given(sasTokenCache.getSasToken(any())).willReturn("token1");

        given(blobContainerClientBuilderProvider.getBlobContainerClientBuilder())
            .willReturn(blobContainerClientBuilder);
        given(blobContainerClientBuilder.containerName(containerName)).willReturn(blobContainerClientBuilder);
        given(blobContainerClientBuilder.sasToken("token1")).willReturn(blobContainerClientBuilder);
        given(blobContainerClientBuilder.buildClient()).willReturn(blobContainerClient);
        given(blobContainerClient.getBlobClient(blobName)).willReturn(blobClient);
        given(blobClient.getBlockBlobClient()).willReturn(blockBlobClient);

        SyncPoller syncPoller = mock(SyncPoller.class);
        given(blockBlobClient.beginCopy(any(), eq(META_DATA_MAP), eq(null), eq(null), eq(null), eq(null),any()))
            .willReturn(syncPoller);

        var pollResponse = mock(PollResponse.class);
        given(syncPoller.waitForCompletion(Duration.ofMinutes(5))).willReturn(pollResponse);
        given(pollResponse.getValue()).willReturn(mock(BlobCopyInfo.class));

        BlobClient sourceBlobClient = mock(BlobClient.class);
        given(sourceBlobClient.getBlobName()).willReturn(blobName);

        String sasToken = "sas_token_01-09-2021";
        given(sourceBlobClient.generateSas(any())).willReturn(sasToken);
        String blobUrl = "http://" + containerName + "/" + blobName;
        given(sourceBlobClient.getBlobUrl()).willReturn(blobUrl);


        blobContainerClientProxy.moveBlob(
            sourceBlobClient,
            containerName,
            TargetStorageAccount.CFT
        );

        verify(sasTokenCache).getSasToken(containerName);

        // then
        ArgumentCaptor<String> copyUrlCap = ArgumentCaptor.forClass(String.class);

        verify(blockBlobClient)
            .beginCopy(copyUrlCap.capture(), any(), eq(null), eq(null), eq(null), eq(null),any());

        verify(blockBlobClient).setMetadata(null);
        verifyNoMoreInteractions(blockBlobClient);

        assertThat(copyUrlCap.getValue()).isEqualTo(blobUrl + "?" + sasToken);

        verify(sasTokenCache, never()).removeFromCache(containerName);

    }

    @Test
    void should_abort_copy_when_there_is_exception() {

        given(sasTokenCache.getSasToken(any())).willReturn("token1");

        given(blobContainerClientBuilderProvider.getBlobContainerClientBuilder())
            .willReturn(blobContainerClientBuilder);
        given(blobContainerClientBuilder.containerName(containerName)).willReturn(blobContainerClientBuilder);
        given(blobContainerClientBuilder.sasToken("token1")).willReturn(blobContainerClientBuilder);
        given(blobContainerClientBuilder.buildClient()).willReturn(blobContainerClient);
        given(blobContainerClient.getBlobClient(blobName)).willReturn(blobClient);
        given(blobClient.getBlockBlobClient()).willReturn(blockBlobClient);

        SyncPoller syncPoller = mock(SyncPoller.class);
        given(blockBlobClient.beginCopy(any(), any(), any(), any(), any(), any(), any()))
            .willReturn(syncPoller);

        var pollResponse = mock(PollResponse.class);
        willThrow(new RuntimeException("Copy Failed"))
            .given(syncPoller).waitForCompletion(Duration.ofMinutes(5));

        var blobCopyInfo = mock(BlobCopyInfo.class);
        given(syncPoller.poll()).willReturn(pollResponse);
        given(pollResponse.getValue()).willReturn(blobCopyInfo);

        String copyId = UUID.randomUUID().toString();
        given(blobCopyInfo.getCopyId()).willReturn(copyId);

        BlobClient sourceBlobClient = mock(BlobClient.class);
        given(sourceBlobClient.getBlobName()).willReturn(blobName);

        String sasToken = "sas_token_01-09-2021";
        given(sourceBlobClient.generateSas(any())).willReturn(sasToken);
        String blobUrl = "http://" + containerName + "/" + blobName;
        given(sourceBlobClient.getBlobUrl()).willReturn(blobUrl);

        assertThatThrownBy(
            () ->   blobContainerClientProxy.moveBlob(
                sourceBlobClient,
                containerName,
                TargetStorageAccount.CFT
            )
        ).isInstanceOf(RuntimeException.class);



        verify(sasTokenCache).getSasToken(containerName);

        // then
        ArgumentCaptor<String> copyUrlCap = ArgumentCaptor.forClass(String.class);

        verify(blockBlobClient)
            .beginCopy(copyUrlCap.capture(), any(), eq(null), eq(null), eq(null), eq(null),any());

        verify(blockBlobClient).abortCopyFromUrl(copyId);

        verify(blockBlobClient, never()).setMetadata(null);
        verifyNoMoreInteractions(blockBlobClient);

        assertThat(copyUrlCap.getValue()).isEqualTo(blobUrl + "?" + sasToken);

        verify(sasTokenCache, never()).removeFromCache(containerName);

    }

    @ParameterizedTest
    @EnumSource(
        value = TargetStorageAccount.class,
        names = {"CFT", "PCQ"}
    )
    void should_invalidate_cache_when_moveBlob_returns_error_response_40x(TargetStorageAccount storageAccount) {
        // given
        HttpResponse mockHttpResponse = mock(HttpResponse.class);
        given(mockHttpResponse.getStatusCode()).willReturn(401);

        if (storageAccount == TargetStorageAccount.PCQ) {
            given(blobContainerClientBuilderProvider.getPcqBlobContainerClientBuilder())
                .willReturn(blobContainerClientBuilder);
        } else if (storageAccount == TargetStorageAccount.CFT) {
            given(blobContainerClientBuilderProvider.getBlobContainerClientBuilder())
                .willReturn(blobContainerClientBuilder);
        }

        willThrow(new BlobStorageException("Sas invalid 401", mockHttpResponse, null))
            .given(blobContainerClientBuilder)
            .sasToken(any());

        BlobClient sourceBlobClient = mock(BlobClient.class);
        given(sourceBlobClient.getBlobName()).willReturn(blobName);

        // then
        assertThatThrownBy(
            () -> blobContainerClientProxy.moveBlob(
                sourceBlobClient,
                containerName,
                storageAccount
            )
        ).isInstanceOf(BlobStorageException.class);
        verify(sasTokenCache).removeFromCache(containerName);

    }

    @Test
    void should_stream_to_crime_storage_when_target_storage_crime() throws Exception {
        given(crimeClient.getBlobClient(blobName)).willReturn(blobClient);
        given(blobClient.getBlockBlobClient()).willReturn(blockBlobClient);


        given(blockBlobClient.commitBlockList(any()))
            .willReturn(mock(BlockBlobItem.class));

        given(sourceBlobClient.getBlockBlobClient()).willReturn(sourceBlockBlobClient);
        given(sourceBlockBlobClient.getBlobName()).willReturn(blobName);

        var envelopeContent = "internal".getBytes();
        var content = getBlobContent(
            Map.of(
                ENVELOPE, envelopeContent,
                SIGNATURE, "sig".getBytes()
            )
        );

        BlobInputStream blobInputStream = mock(
            BlobInputStream.class,
            AdditionalAnswers.delegatesTo(new ByteArrayInputStream(content))
        );

        given(sourceBlockBlobClient.openInputStream()).willReturn(blobInputStream);

        blobContainerClientProxy.streamContentToDestination(
            sourceBlobClient,
            containerName,
            TargetStorageAccount.CRIME
        );

        // then

        verify(blockBlobClient).stageBlock(any(), any(), eq(8L));
        verify(blockBlobClient).commitBlockList(any());
        verify(blockBlobClient).getBlobUrl();
        verifyNoMoreInteractions(blockBlobClient);
        verify(sasTokenCache, never()).getSasToken(containerName);
    }


    @Test
    void should_throw_exception_when_envelope_zip_not_found() throws Exception {
        given(sourceBlobClient.getBlockBlobClient()).willReturn(sourceBlockBlobClient);
        given(sourceBlockBlobClient.getBlobName()).willReturn(blobName);

        var content = getBlobContent(
            Map.of(
                SIGNATURE, "sig".getBytes()
            )
        );

        BlobInputStream blobInputStream = mock(
            BlobInputStream.class,
            AdditionalAnswers.delegatesTo(new ByteArrayInputStream(content))
        );

        given(sourceBlockBlobClient.openInputStream()).willReturn(blobInputStream);

        assertThrows(
            InvalidZipArchiveException.class,
            () -> blobContainerClientProxy.streamContentToDestination(
                sourceBlobClient,
                containerName,
                TargetStorageAccount.CRIME
            )
        );
        // then
        verify(sasTokenCache, never()).getSasToken(containerName);
    }

    @Test
    void should_throw_exception_when_chunk_upload_fails() throws Exception {
        given(sourceBlobClient.getBlockBlobClient()).willReturn(sourceBlockBlobClient);
        given(sourceBlockBlobClient.getBlobName()).willReturn(blobName);

        var content = getBlobContent(
            Map.of(
                ENVELOPE, "envelopeContent".getBytes(),
                SIGNATURE, "sig".getBytes()
            )
        );

        BlobInputStream blobInputStream = mock(
            BlobInputStream.class,
            AdditionalAnswers.delegatesTo(new ByteArrayInputStream(content))
        );
        given(sourceBlockBlobClient.openInputStream()).willReturn(blobInputStream);

        given(crimeClient.getBlobClient(blobName)).willReturn(blobClient);
        given(blobClient.getBlockBlobClient()).willReturn(blockBlobClient);

        HttpResponse mockHttpResponse = mock(HttpResponse.class);
        willThrow(new BlobStorageException("Stage upload failed", mockHttpResponse, null))
            .given(blockBlobClient)
            .stageBlock(any(), any(), anyLong());

        assertThrows(
            BlobStorageException.class,
            () -> blobContainerClientProxy.streamContentToDestination(
                sourceBlobClient,
                containerName,
                TargetStorageAccount.CRIME
            )
        );

        verify(blockBlobClient).delete();
    }

    @Test
    void should_not_throw_exception_when_chunk_upload_delete_fails() throws Exception {
        given(sourceBlobClient.getBlockBlobClient()).willReturn(sourceBlockBlobClient);
        given(sourceBlockBlobClient.getBlobName()).willReturn(blobName);

        var content = getBlobContent(
            Map.of(
                ENVELOPE, "envelopeContent".getBytes(),
                SIGNATURE, "sig".getBytes()
            )
        );

        BlobInputStream blobInputStream = mock(
            BlobInputStream.class,
            AdditionalAnswers.delegatesTo(new ByteArrayInputStream(content))
        );
        given(sourceBlockBlobClient.openInputStream()).willReturn(blobInputStream);

        given(crimeClient.getBlobClient(blobName)).willReturn(blobClient);
        given(blobClient.getBlockBlobClient()).willReturn(blockBlobClient);


        HttpResponse mockHttpResponse = mock(HttpResponse.class);
        willThrow(new BlobStorageException("Stage upload failed", mockHttpResponse, null))
            .given(blockBlobClient)
            .stageBlock(any(), any(), anyLong());


        willThrow(new RuntimeException("Delete error"))
            .given(blockBlobClient)
            .delete();

        assertThrows(
            BlobStorageException.class,
            () -> blobContainerClientProxy.streamContentToDestination(
                sourceBlobClient,
                containerName,
                TargetStorageAccount.CRIME
            )
        );

        verify(blockBlobClient).delete();
    }

    @ParameterizedTest
    @EnumSource(
        value = TargetStorageAccount.class,
        names = {"CFT", "PCQ"}
    )
    void should_invalidate_cache_when_streamBlob_returns_error_response_40x(TargetStorageAccount storageAccount)
        throws IOException {
        // given
        HttpResponse mockHttpResponse = mock(HttpResponse.class);
        given(mockHttpResponse.getStatusCode()).willReturn(401);

        if (storageAccount == TargetStorageAccount.PCQ) {
            given(blobContainerClientBuilderProvider.getPcqBlobContainerClientBuilder())
                .willReturn(blobContainerClientBuilder);
        } else if (storageAccount == TargetStorageAccount.CFT) {
            given(blobContainerClientBuilderProvider.getBlobContainerClientBuilder())
                .willReturn(blobContainerClientBuilder);
        }

        willThrow(new BlobStorageException("Sas invalid 401", mockHttpResponse, null))
            .given(blobContainerClientBuilder)
            .sasToken(any());


        given(sourceBlobClient.getBlockBlobClient()).willReturn(sourceBlockBlobClient);
        var content = getBlobContent(
            Map.of(
                ENVELOPE, "envelopeContent".getBytes(),
                SIGNATURE, "sig".getBytes()
            )
        );

        BlobInputStream blobInputStream1 = mock(
            BlobInputStream.class,
            AdditionalAnswers.delegatesTo(new ByteArrayInputStream(content))
        );

        BlobInputStream blobInputStream2 = mock(
            BlobInputStream.class,
            AdditionalAnswers.delegatesTo(new ByteArrayInputStream(content))
        );
        given(sourceBlockBlobClient.openInputStream()).willReturn(blobInputStream1, blobInputStream2);

        given(sourceBlockBlobClient.getBlobName()).willReturn(blobName);
        // then
        assertThrows(
            BlobStorageException.class,
            () -> blobContainerClientProxy.streamContentToDestination(
                sourceBlobClient,
                containerName,
                storageAccount
            )
        );

        verify(sasTokenCache).removeFromCache(containerName);

    }

    private static byte[] getBlobContent(Map<String, byte[]> zipEntries) throws IOException {
        try (
            var outputStream = new ByteArrayOutputStream();
            var zipOutputStream = new ZipOutputStream(outputStream)
        ) {
            for (var entry : zipEntries.entrySet()) {
                zipOutputStream.putNextEntry(new ZipEntry(entry.getKey()));
                zipOutputStream.write(entry.getValue());
                zipOutputStream.closeEntry();
            }

            return outputStream.toByteArray();
        }
    }
}
