package uk.gov.hmcts.reform.blobrouter.tasks.processors;

import com.azure.core.test.http.MockHttpResponse;
import com.azure.storage.blob.BlobClient;
import com.azure.storage.blob.BlobContainerClient;
import com.azure.storage.blob.BlobServiceClient;
import com.azure.storage.blob.models.BlobStorageException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.reform.blobrouter.data.envelopes.Envelope;
import uk.gov.hmcts.reform.blobrouter.data.envelopes.Status;
import uk.gov.hmcts.reform.blobrouter.services.EnvelopeService;

import java.time.Instant;
import java.util.UUID;

import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static uk.gov.hmcts.reform.blobrouter.data.envelopes.Status.DISPATCHED;

@ExtendWith(MockitoExtension.class)
class ContainerCleanerTest {

    private static final String CONTAINER_NAME = "bulkscan";

    private ContainerCleaner containerCleaner;

    @Mock EnvelopeService envelopeService;
    @Mock BlobServiceClient storageClient;
    @Mock BlobContainerClient containerClient;
    @Mock BlobClient blobClient1;
    @Mock BlobClient blobClient2;

    private static final Envelope ENVELOPE_1 = createEnvelope(UUID.randomUUID(), DISPATCHED, "file1.zip");
    private static final Envelope ENVELOPE_2 = createEnvelope(UUID.randomUUID(), DISPATCHED, "file2.zip");

    @BeforeEach
    void setUp() {
        containerCleaner = new ContainerCleaner(storageClient, envelopeService);

        given(storageClient.getBlobContainerClient(CONTAINER_NAME)).willReturn(containerClient);
    }

    @Test
    void should_not_find_any_blobs_when_no_db_results() {
        // given
        given(envelopeService.getReadyToDeleteDispatches(CONTAINER_NAME)).willReturn(emptyList());

        // when
        containerCleaner.process(CONTAINER_NAME);

        // then
        verifyNoInteractions(containerClient);
    }

    @Test
    void should_handle_repository_exception() {
        // given
        given(envelopeService.getReadyToDeleteDispatches(CONTAINER_NAME)).willThrow(new RuntimeException("msg"));

        // when
        containerCleaner.process(CONTAINER_NAME);

        // then
        verifyNoInteractions(containerClient);
    }

    @Test
    void should_find_blobs_delete_and_update_in_db() {
        // given
        given(envelopeService.getReadyToDeleteDispatches(CONTAINER_NAME))
            .willReturn(asList(
                ENVELOPE_1,
                ENVELOPE_2
            ));
        given(containerClient.getBlobClient(ENVELOPE_1.fileName)).willReturn(blobClient1);
        given(containerClient.getBlobClient(ENVELOPE_2.fileName)).willReturn(blobClient2);

        // when
        containerCleaner.process(CONTAINER_NAME);

        // then
        verify(containerClient).getBlobClient(ENVELOPE_1.fileName);
        verify(containerClient).getBlobClient(ENVELOPE_2.fileName);
        verify(containerClient, times(2)).getBlobContainerName();
        verifyNoMoreInteractions(containerClient);
        verify(blobClient1).delete();
        verify(blobClient2).delete();
        verify(envelopeService).markEnvelopeAsDeleted(ENVELOPE_1);
        verify(envelopeService).markEnvelopeAsDeleted(ENVELOPE_2);
        verifyNoMoreInteractions(envelopeService);
    }

    @Test
    void should_handle_non_existing_file() {
        // given
        given(envelopeService.getReadyToDeleteDispatches(CONTAINER_NAME))
            .willReturn(singletonList(
                ENVELOPE_1
            ));
        given(containerClient.getBlobClient(ENVELOPE_1.fileName)).willReturn(blobClient1);
        doThrow(new BlobStorageException("msg", new MockHttpResponse(null, 404), null))
            .when(blobClient1).delete();

        // when
        assertThatCode(() -> containerCleaner.process(CONTAINER_NAME)).doesNotThrowAnyException();

        // then
        verify(containerClient).getBlobClient(ENVELOPE_1.fileName);
        verify(containerClient).getBlobContainerName();
        verifyNoMoreInteractions(containerClient);
        verify(blobClient1).delete();
        verify(envelopeService).markEnvelopeAsDeleted(ENVELOPE_1);
        verifyNoMoreInteractions(envelopeService);
    }

    @Test
    void should_handle_server_error() {
        // given
        given(envelopeService.getReadyToDeleteDispatches(CONTAINER_NAME))
            .willReturn(singletonList(
                ENVELOPE_1
            ));
        given(containerClient.getBlobClient(ENVELOPE_1.fileName)).willReturn(blobClient1);
        doThrow(new BlobStorageException("msg", new MockHttpResponse(null, 500), null))
            .when(blobClient1).delete();

        // when
        assertThatCode(() -> containerCleaner.process(CONTAINER_NAME)).doesNotThrowAnyException();

        // then
        verify(containerClient).getBlobClient(ENVELOPE_1.fileName);
        verify(containerClient).getBlobContainerName();
        verifyNoMoreInteractions(containerClient);
        verify(blobClient1).delete();
        verifyNoMoreInteractions(envelopeService);
    }

    private static Envelope createEnvelope(UUID uuid, Status status, String fileName) {
        return new Envelope(
            uuid,
            CONTAINER_NAME,
            fileName,
            Instant.now(),
            Instant.now(),
            Instant.now(),
            status,
            false,
            false
        );
    }
}
