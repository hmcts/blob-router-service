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
import uk.gov.hmcts.reform.blobrouter.data.EnvelopeRepositoryImpl;
import uk.gov.hmcts.reform.blobrouter.data.model.Envelope;
import uk.gov.hmcts.reform.blobrouter.data.model.Status;

import java.time.Instant;
import java.util.UUID;

import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static uk.gov.hmcts.reform.blobrouter.data.model.Status.DISPATCHED;

@ExtendWith(MockitoExtension.class)
class ContainerCleanerTest {

    private static final String CONTAINER_NAME = "bulkscan";

    private ContainerCleaner containerCleaner;

    @Mock
    private EnvelopeRepositoryImpl envelopeRepository;

    private BlobServiceClient storageClient = mock(BlobServiceClient.class);

    @Mock
    private BlobContainerClient containerClient;

    @Mock
    private BlobClient blobClient1;

    @Mock
    private BlobClient blobClient2;

    @Mock
    private BlobClient blobClient3;

    @Mock
    private BlobClient blobClient4;

    private static final UUID UUID_1 = UUID.randomUUID();
    private static final UUID UUID_2 = UUID.randomUUID();

    private static final String FILE_NAME_1 = "file1.zip";
    private static final String FILE_NAME_2 = "file2.zip";

    private static final Envelope ENVELOPE_1 = createEnvelope(UUID_1, DISPATCHED, FILE_NAME_1);
    private static final Envelope ENVELOPE_2 = createEnvelope(UUID_2, DISPATCHED, FILE_NAME_2);

    @BeforeEach
    void setUp() {
        containerCleaner = new ContainerCleaner(storageClient, envelopeRepository);

        given(storageClient.getBlobContainerClient(CONTAINER_NAME)).willReturn(containerClient);
    }

    @Test
    void should_not_find_any_blobs() {
        // given
        given(envelopeRepository.find(DISPATCHED, false)).willReturn(emptyList());

        // when
        containerCleaner.process(CONTAINER_NAME);

        // then
        verifyNoInteractions(containerClient);
    }

    @Test
    void should_find_blobs_and_delete() {
        // given
        given(envelopeRepository.find(DISPATCHED, false))
            .willReturn(asList(
                ENVELOPE_1,
                ENVELOPE_2
            ));
        given(containerClient.getBlobClient(FILE_NAME_1)).willReturn(blobClient1);
        given(containerClient.getBlobClient(FILE_NAME_2)).willReturn(blobClient2);

        // when
        containerCleaner.process(CONTAINER_NAME);

        // then
        verify(containerClient).getBlobClient(FILE_NAME_1);
        verify(containerClient).getBlobClient(FILE_NAME_2);
        verify(containerClient, times(2)).getBlobContainerName();
        verifyNoMoreInteractions(containerClient);
        verify(blobClient1).delete();
        verify(blobClient2).delete();
        verify(envelopeRepository).find(DISPATCHED, false);
        verify(envelopeRepository).markAsDeleted(UUID_1);
        verify(envelopeRepository).markAsDeleted(UUID_2);
        verifyNoMoreInteractions(envelopeRepository);
    }

    @Test
    void should_handle_non_existing_file() {
        // given
        given(envelopeRepository.find(DISPATCHED, false))
            .willReturn(singletonList(
                ENVELOPE_1
            ));
        given(containerClient.getBlobClient(FILE_NAME_1)).willReturn(blobClient1);
        doThrow(new BlobStorageException("msg", new MockHttpResponse(null, 404), null))
            .when(blobClient1).delete();

        // when
        assertThatCode(() -> containerCleaner.process(CONTAINER_NAME)).doesNotThrowAnyException();

        // then
        verify(containerClient).getBlobClient(FILE_NAME_1);
        verify(containerClient).getBlobContainerName();
        verifyNoMoreInteractions(containerClient);
        verify(blobClient1).delete();
        verify(envelopeRepository).find(DISPATCHED, false);
        verify(envelopeRepository).markAsDeleted(UUID_1);
        verifyNoMoreInteractions(envelopeRepository);
    }

    @Test
    void should_handle_server_error() {
        // given
        // given
        given(envelopeRepository.find(DISPATCHED, false))
            .willReturn(singletonList(
                ENVELOPE_1
            ));
        given(containerClient.getBlobClient(FILE_NAME_1)).willReturn(blobClient1);
        doThrow(new BlobStorageException("msg", new MockHttpResponse(null, 500), null))
            .when(blobClient1).delete();

        // when
        assertThatCode(() -> containerCleaner.process(CONTAINER_NAME)).doesNotThrowAnyException();

        // then
        verify(containerClient).getBlobClient(FILE_NAME_1);
        verify(containerClient).getBlobContainerName();
        verifyNoMoreInteractions(containerClient);
        verify(blobClient1).delete();
        verify(envelopeRepository).find(DISPATCHED, false);
        verifyNoMoreInteractions(envelopeRepository);
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
            false
        );
    }
}
