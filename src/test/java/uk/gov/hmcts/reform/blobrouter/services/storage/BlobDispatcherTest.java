package uk.gov.hmcts.reform.blobrouter.services.storage;

import com.azure.storage.blob.BlobClient;
import com.azure.storage.blob.BlobContainerClient;
import com.azure.storage.blob.models.BlobStorageException;
import com.azure.storage.blob.specialized.BlockBlobClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.ByteArrayInputStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.verify;
import static uk.gov.hmcts.reform.blobrouter.config.TargetStorageAccount.BULKSCAN;

@ExtendWith(MockitoExtension.class)
class BlobDispatcherTest {

    @Mock BlobContainerClientProvider blobContainerClientProvider;
    @Mock BlobContainerClient blobContainerClient;
    @Mock BlobClient blobClient;
    @Mock BlockBlobClient blockBlobClient;

    private BlobDispatcher dispatcher;

    @BeforeEach
    void setUp() {
        dispatcher = new BlobDispatcher(blobContainerClientProvider);
    }

    @Test
    void should_use_blob_client_to_dispatch_file() {
        // given
        final String blobName = "hello.zip";
        final byte[] blobContent = "some data".getBytes();
        final String container = "container";

        given(blobContainerClientProvider.get(BULKSCAN, container)).willReturn(blobContainerClient);
        given(blobContainerClient.getBlobClient(blobName)).willReturn(blobClient);
        given(blobClient.getBlockBlobClient()).willReturn(blockBlobClient);

        // when
        dispatcher.dispatch(blobName, blobContent, container, BULKSCAN);

        // then
        ArgumentCaptor<ByteArrayInputStream> data = ArgumentCaptor.forClass(ByteArrayInputStream.class);

        verify(blockBlobClient)
            .upload(
                data.capture(),
                eq(Long.valueOf(blobContent.length))
            );

        assertThat(data.getValue().readAllBytes()).isEqualTo(blobContent);
    }

    @Test
    void should_rethrow_exceptions() {
        // given
        willThrow(new BlobStorageException("test exception", null, null))
            .given(blobContainerClientProvider)
            .get(any(), any());

        // when
        Throwable exc = catchThrowable(
            () -> dispatcher.dispatch("foo.zip", "data".getBytes(), "some_container", BULKSCAN)
        );

        // then
        assertThat(exc).isInstanceOf(BlobStorageException.class);
    }
}
