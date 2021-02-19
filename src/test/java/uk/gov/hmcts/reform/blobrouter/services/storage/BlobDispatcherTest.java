package uk.gov.hmcts.reform.blobrouter.services.storage;

import com.azure.storage.blob.BlobClient;
import com.azure.storage.blob.BlobContainerClient;
import com.azure.storage.blob.models.BlobStorageException;
import com.azure.storage.blob.specialized.BlobInputStream;
import com.azure.storage.blob.specialized.BlockBlobClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.AdditionalAnswers;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.reform.blobrouter.exceptions.InvalidZipArchiveException;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static uk.gov.hmcts.reform.blobrouter.config.TargetStorageAccount.CFT;
import static uk.gov.hmcts.reform.blobrouter.config.TargetStorageAccount.CRIME;
import static uk.gov.hmcts.reform.blobrouter.util.zipverification.ZipVerifiers.ENVELOPE;
import static uk.gov.hmcts.reform.blobrouter.util.zipverification.ZipVerifiers.SIGNATURE;

@ExtendWith(MockitoExtension.class)
class BlobDispatcherTest {

    @Mock BlobContainerClientProxy blobContainerClientProxy;
    @Mock BlobContainerClient blobContainerClient;
    @Mock BlobClient blobClient;
    @Mock BlockBlobClient blockBlobClient;
    @Mock BlobMover blobMover;

    private BlobDispatcher dispatcher;

    @BeforeEach
    void setUp() {
        dispatcher = new BlobDispatcher(blobContainerClientProxy, blobMover);
    }

    @Test
    void should_use_blob_client_to_dispatch_file() throws IOException {
        // given
        final String blobName = "hello.zip";
        final String container = "container";
        var content = getBlobContent(
            Map.of(
                ENVELOPE, "content".getBytes(),
                SIGNATURE, "sig".getBytes()
            )
        );

        BlobInputStream blobInputStream = mock(
            BlobInputStream.class,
            AdditionalAnswers.delegatesTo(new ByteArrayInputStream(content))
        );
        given(blobClient.getBlockBlobClient()).willReturn(blockBlobClient);
        given(blockBlobClient.openInputStream())
            .willReturn(blobInputStream);

        doNothing().when(blobContainerClientProxy)
            .runUpload(eq(blockBlobClient), eq(container), eq(CRIME), any());

        // when
        dispatcher.dispatch(blobClient, container, CRIME);

        // then
        verify(blobContainerClientProxy)
            .runUpload(any(), any(), any(), any());
    }

    @Test
    void should_rethrow_exceptions() throws IOException {
        // given
        given(blobClient.getBlockBlobClient()).willReturn(blockBlobClient);

        var content = getBlobContent(
            Map.of(
                ENVELOPE, "content".getBytes(),
                SIGNATURE, "sig".getBytes()
            )
        );

        BlobInputStream blobInputStream = mock(
            BlobInputStream.class,
            AdditionalAnswers.delegatesTo(new ByteArrayInputStream(content))
        );

        given(blockBlobClient.openInputStream())
            .willReturn(blobInputStream);

        willThrow(new BlobStorageException("test exception", null, null))
            .given(blobContainerClientProxy)
            .runUpload(any(), any(), any(),any());

        // when
        Throwable exc = catchThrowable(
            () -> dispatcher.dispatch(blobClient, "some_container", CFT)
        );

        // then
        assertThat(exc).isInstanceOf(BlobStorageException.class);
    }

    @Test
    void should_throw_InvalidZipArchiveException_if_there_is_no_inner_envelope() throws IOException {
        // given
        given(blobClient.getBlockBlobClient()).willReturn(blockBlobClient);

        var content = getBlobContent(
            Map.of(
                SIGNATURE, "sig".getBytes()
            )
        );

        BlobInputStream blobInputStream = mock(
            BlobInputStream.class,
            AdditionalAnswers.delegatesTo(new ByteArrayInputStream(content))
        );

        given(blockBlobClient.openInputStream())
            .willReturn(blobInputStream);

        // when
        Throwable exc = catchThrowable(
            () -> dispatcher.dispatch(blobClient, "some_container", CFT)
        );

        // then
        assertThat(exc).isInstanceOf(InvalidZipArchiveException.class);
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
