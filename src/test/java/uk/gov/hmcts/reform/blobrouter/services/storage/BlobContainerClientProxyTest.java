package uk.gov.hmcts.reform.blobrouter.services.storage;

import com.azure.core.http.HttpResponse;
import com.azure.storage.blob.BlobClient;
import com.azure.storage.blob.BlobContainerClient;
import com.azure.storage.blob.BlobContainerClientBuilder;
import com.azure.storage.blob.models.BlobStorageException;
import com.azure.storage.blob.specialized.BlockBlobClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.reform.blobrouter.config.TargetStorageAccount;

import java.io.ByteArrayInputStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
public class BlobContainerClientProxyTest {

    @Mock BlobContainerClient crimeClient;
    @Mock BulkScanSasTokenCache bulkScanSasTokenCache;
    @Mock BlobContainerClientBuilder blobContainerClientBuilder;
    @Mock BlobContainerClientBuilderProvider blobContainerClientBuilderProvider;

    BlobContainerClientProxy blobContainerClientProxy;

    @Mock BlobContainerClient blobContainerClient;
    @Mock BlobClient blobClient;
    @Mock BlockBlobClient blockBlobClient;

    final String containerName = "container123";
    final String blobName = "hello.zip";
    final byte[] blobContent = "some data".getBytes();

    @BeforeEach
    private void setUp() {
        this.blobContainerClientProxy = new BlobContainerClientProxy(
            crimeClient,
            blobContainerClientBuilderProvider,
            bulkScanSasTokenCache
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
            TargetStorageAccount.CRIME);

        // then
        ArgumentCaptor<ByteArrayInputStream> data = ArgumentCaptor.forClass(ByteArrayInputStream.class);

        verify(blockBlobClient)
            .upload(
                data.capture(),
                eq(Long.valueOf(blobContent.length))
            );

        assertThat(data.getValue().readAllBytes()).isEqualTo(blobContent);

        verify(bulkScanSasTokenCache, never()).getSasToken(containerName);
    }

    @Test
    void should_upload_to_bulk_scan_storage_when_target_storage_bulk_scan() {

        given(bulkScanSasTokenCache.getSasToken(any())).willReturn("token1");

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
            TargetStorageAccount.BULKSCAN);

        verify(bulkScanSasTokenCache).getSasToken(containerName);

        // then
        ArgumentCaptor<ByteArrayInputStream> data = ArgumentCaptor.forClass(ByteArrayInputStream.class);

        verify(blockBlobClient)
            .upload(
                data.capture(),
                eq(Long.valueOf(blobContent.length))
            );

        assertThat(data.getValue().readAllBytes()).isEqualTo(blobContent);

        verify(bulkScanSasTokenCache, never()).removeFromCache(containerName);

    }

    @Test
    void should_invalidate_cache_when_target_storage_bulk_scan_and_error_response_40x() {

        HttpResponse mockHttpResponse = mock(HttpResponse.class);
        given(mockHttpResponse.getStatusCode()).willReturn(401);

        given(blobContainerClientBuilderProvider.getBlobContainerClientBuilder())
            .willReturn(blobContainerClientBuilder);
        given(blobContainerClientBuilder.sasToken(any())).willThrow(
            new BlobStorageException("Sas invalid 401", mockHttpResponse, null));

        assertThatThrownBy(
            () -> blobContainerClientProxy.upload(
                blobName,
                blobContent,
                containerName,
                TargetStorageAccount.BULKSCAN
            )
        ).isInstanceOf(BlobStorageException.class);

        verify(bulkScanSasTokenCache).removeFromCache(containerName);

    }

    @Test
    void should_not_invalidate_cache_when_target_storage_crime_and_error_response_40x() {

        HttpResponse mockHttpResponse = mock(HttpResponse.class);
        given(crimeClient.getBlobClient(any())).willThrow(
            new BlobStorageException("Sas invalid 401", mockHttpResponse, null));

        assertThatThrownBy(
            () -> blobContainerClientProxy.upload(
                blobName,
                blobContent,
                containerName,
                TargetStorageAccount.CRIME
            )
        ).isInstanceOf(BlobStorageException.class);

        verify(bulkScanSasTokenCache, never()).removeFromCache(containerName);

    }
}
