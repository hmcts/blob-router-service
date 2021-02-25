package uk.gov.hmcts.reform.blobrouter.services.storage;

import com.azure.core.http.HttpResponse;
import com.azure.storage.blob.BlobClient;
import com.azure.storage.blob.BlobContainerClient;
import com.azure.storage.blob.BlobContainerClientBuilder;
import com.azure.storage.blob.models.BlobStorageException;
import com.azure.storage.blob.specialized.BlockBlobClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.reform.blobrouter.config.TargetStorageAccount;

import java.util.function.Consumer;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@SuppressWarnings("unchecked")
@ExtendWith(MockitoExtension.class)
class BlobContainerClientProxyTest {

    @Mock BlobContainerClient crimeClient;
    @Mock SasTokenCache sasTokenCache;
    @Mock BlobContainerClientBuilder blobContainerClientBuilder;
    @Mock BlobContainerClientBuilderProvider blobContainerClientBuilderProvider;

    BlobContainerClientProxy blobContainerClientProxy;

    @Mock BlobContainerClient blobContainerClient;
    @Mock BlobClient blobClient;
    @Mock BlockBlobClient sourceBlockBlobClient;

    @Mock BlockBlobClient blockBlobClient;

    final String containerName = "container123";
    final String blobName = "hello.zip";

    @BeforeEach
    private void setUp() {
        this.blobContainerClientProxy = new BlobContainerClientProxy(
            crimeClient,
            blobContainerClientBuilderProvider,
            sasTokenCache
        );
    }

    @ParameterizedTest
    @EnumSource(
        value = TargetStorageAccount.class
    )
    void should_run_provided_action_if_target_blob_client_created(TargetStorageAccount storageAccount) {
        // given
        given(sourceBlockBlobClient.getBlobName()).willReturn(blobName);

        if (storageAccount == TargetStorageAccount.PCQ) {
            given(blobContainerClientBuilderProvider.getPcqBlobContainerClientBuilder())
                .willReturn(blobContainerClientBuilder);
            given(sasTokenCache.getPcqSasToken(any())).willReturn("token1");
        } else if (storageAccount == TargetStorageAccount.CFT) {
            given(blobContainerClientBuilderProvider.getBlobContainerClientBuilder())
                .willReturn(blobContainerClientBuilder);
            given(sasTokenCache.getSasToken(any())).willReturn("token1");
        }

        if (storageAccount == TargetStorageAccount.CRIME) {
            blobContainerClient = crimeClient;
        } else {
            given(blobContainerClientBuilder.sasToken("token1"))
                .willReturn(blobContainerClientBuilder);
            given(blobContainerClientBuilder.containerName(containerName))
                .willReturn(blobContainerClientBuilder);
            given(blobContainerClientBuilder.buildClient()).willReturn(blobContainerClient);
        }

        given(blobContainerClient.getBlobClient(blobName)).willReturn(blobClient);
        given(blobClient.getBlockBlobClient()).willReturn(blockBlobClient);
        var uploadMethod = mock(Consumer.class);

        // when
        blobContainerClientProxy.runUpload(
            sourceBlockBlobClient,
            containerName,
            storageAccount,
            uploadMethod
        );
        // then
        verify(uploadMethod).accept(blockBlobClient);
    }

    @ParameterizedTest
    @EnumSource(
        value = TargetStorageAccount.class
    )
    void should_not_run_provided_action_if_there_is_error(TargetStorageAccount storageAccount) {
        // given
        willThrow(new RuntimeException("Get Name error"))
            .given(sourceBlockBlobClient)
            .getBlobName();
        var uploadMethod = mock(Consumer.class);
        // when
        assertThrows(
            RuntimeException.class,
            () -> blobContainerClientProxy.runUpload(
                sourceBlockBlobClient,
                containerName,
                storageAccount,
                uploadMethod
            )
        );
        // then
        verify(uploadMethod, never()).accept(blockBlobClient);
        verify(sasTokenCache, never()).removeFromCache(any());

    }

    @ParameterizedTest
    @EnumSource(
        value = TargetStorageAccount.class,
        names = {"CFT", "PCQ"}
    )
    void should_invalidate_cache_when_runUpload_fails_with_40x(TargetStorageAccount storageAccount) {
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
        var uploadMethod = mock(Consumer.class);
        // when
        assertThrows(
            BlobStorageException.class,
            () -> blobContainerClientProxy.runUpload(
                sourceBlockBlobClient,
                containerName,
                storageAccount,
                uploadMethod
            )
        );
        // then
        verify(uploadMethod, never()).accept(blockBlobClient);
        verify(sasTokenCache).removeFromCache(any());
    }
}
