package uk.gov.hmcts.reform.blobrouter.tasks.processors;

import com.azure.core.http.HttpResponse;
import com.azure.core.util.Context;
import com.azure.storage.blob.BlobClient;
import com.azure.storage.blob.BlobContainerClient;
import com.azure.storage.blob.BlobServiceClient;
import com.azure.storage.blob.models.BlobStorageException;
import com.azure.storage.blob.models.DeleteSnapshotsOptionType;
import com.azure.storage.blob.specialized.BlobLeaseClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.reform.blobrouter.data.envelopes.Envelope;
import uk.gov.hmcts.reform.blobrouter.data.envelopes.Status;
import uk.gov.hmcts.reform.blobrouter.services.EnvelopeService;
import uk.gov.hmcts.reform.blobrouter.services.storage.BlobMetaDataHandler;
import uk.gov.hmcts.reform.blobrouter.services.storage.LeaseAcquirer;

import java.time.Instant;
import java.util.UUID;

import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static uk.gov.hmcts.reform.blobrouter.data.envelopes.Status.DISPATCHED;

@ExtendWith(MockitoExtension.class)
class ContainerCleanerTest {

    private static final String CONTAINER_NAME = "bulkscan";

    private ContainerCleaner containerCleaner;

    @Mock EnvelopeService envelopeService;
    @Mock HttpResponse httpResponse;
    @Mock BlobServiceClient storageClient;
    @Mock BlobContainerClient containerClient;
    @Mock BlobClient blobClient1;
    @Mock BlobClient blobClient2;
    @Mock BlobLeaseClient leaseClient;
    @Mock BlobMetaDataHandler blobMetaDataHandler;

    private static final Envelope ENVELOPE_1 = createEnvelope(UUID.randomUUID(), DISPATCHED, "file1.zip");
    private static final Envelope ENVELOPE_2 = createEnvelope(UUID.randomUUID(), DISPATCHED, "file2.zip");

    @BeforeEach
    void setUp() {
        containerCleaner = new ContainerCleaner(
            storageClient,
            envelopeService,
            new LeaseAcquirer(blobClient -> leaseClient, blobMetaDataHandler)
        );

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
        String leaseId1 = UUID.randomUUID().toString();
        String leaseId2 = UUID.randomUUID().toString();
        given(leaseClient.acquireLease(LeaseAcquirer.LEASE_DURATION_IN_SECONDS)).willReturn(leaseId1, leaseId2);
        given(blobMetaDataHandler.isBlobReadyToUse(blobClient1, leaseId1)).willReturn(true);
        given(blobMetaDataHandler.isBlobReadyToUse(blobClient2, leaseId2)).willReturn(true);
        // when
        containerCleaner.process(CONTAINER_NAME);

        // then
        verify(containerClient).getBlobClient(ENVELOPE_1.fileName);
        verify(containerClient).getBlobClient(ENVELOPE_2.fileName);
        verifyNoMoreInteractions(containerClient);
        verify(blobClient1).getContainerName();
        verify(blobClient2).getContainerName();

        // and

        verify(blobClient1).deleteWithResponse(
            eq(DeleteSnapshotsOptionType.INCLUDE),
            eq(null),
            eq(null),
            eq(Context.NONE)
        );
        verify(blobClient2).deleteWithResponse(
            eq(DeleteSnapshotsOptionType.INCLUDE),
            eq(null),
            eq(null),
            eq(Context.NONE)
        );

        // and
        verify(envelopeService).markEnvelopeAsDeleted(ENVELOPE_1);
        verify(envelopeService).markEnvelopeAsDeleted(ENVELOPE_2);
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
        String leaseId = UUID.randomUUID().toString();
        given(leaseClient.acquireLease(LeaseAcquirer.LEASE_DURATION_IN_SECONDS))
            .willReturn(leaseId);
        given(blobMetaDataHandler.isBlobReadyToUse(blobClient1, leaseId)).willReturn(true);

        doThrow(new BlobStorageException("msg", httpResponse, null))
            .when(blobClient1).deleteWithResponse(any(), any(), eq(null), eq(Context.NONE));

        // when
        assertThatCode(() -> containerCleaner.process(CONTAINER_NAME)).doesNotThrowAnyException();

        // then
        verify(containerClient).getBlobClient(ENVELOPE_1.fileName);
        verifyNoMoreInteractions(containerClient);
        verify(blobClient1).getContainerName();
        verify(blobClient1).deleteWithResponse(any(), eq(null), eq(null), eq(Context.NONE));
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
