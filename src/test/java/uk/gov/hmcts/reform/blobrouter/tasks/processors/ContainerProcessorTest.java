package uk.gov.hmcts.reform.blobrouter.tasks.processors;

import com.azure.core.http.rest.PagedIterable;
import com.azure.storage.blob.BlobClient;
import com.azure.storage.blob.BlobContainerClient;
import com.azure.storage.blob.BlobServiceClient;
import com.azure.storage.blob.models.BlobErrorCode;
import com.azure.storage.blob.models.BlobItem;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.reform.blobrouter.data.envelopes.Envelope;
import uk.gov.hmcts.reform.blobrouter.data.envelopes.Status;
import uk.gov.hmcts.reform.blobrouter.services.EnvelopeService;
import uk.gov.hmcts.reform.blobrouter.services.storage.LeaseAcquirer;

import java.util.Optional;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.stream.Stream;

import static java.time.Instant.now;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;

@ExtendWith(MockitoExtension.class)
class ContainerProcessorTest {

    @Mock BlobServiceClient storageClient;
    @Mock BlobProcessor blobProcessor;
    @Mock LeaseAcquirer leaseAcquirer;
    @Mock EnvelopeService envelopeService;

    @Mock BlobContainerClient containerClient;
    @Mock BlobClient blobClient;
    @Mock PagedIterable<BlobItem> listBlobsResult;

    ContainerProcessor containerProcessor;

    @BeforeEach
    void setUp() {
        containerProcessor = new ContainerProcessor(
            storageClient,
            blobProcessor,
            leaseAcquirer,
            envelopeService
        );
    }

    @Test
    void should_continue_processing_blob_for_which_envelope_in_created_status_exists() {
        // given
        var envelope = envelope(Status.CREATED);
        storageHasBlob(envelope.fileName, envelope.container);
        leaseCanBeAcquired();
        given(envelopeService.findEnvelopeNotInCreatedStatus(envelope.fileName, envelope.container))
            .willReturn(Optional.empty());
        // when
        containerProcessor.process(envelope.container);

        // then
        verify(blobProcessor).process(blobClient);
        verifyNoMoreInteractions(blobProcessor);
    }

    @Test
    void should_skip_blob_if_corresponding_envelope_is_not_in_created_status() {
        // given
        var envelope = envelope(Status.DISPATCHED);
        storageHasBlob(envelope.fileName, envelope.container);
        dbHas(envelope);

        // when
        containerProcessor.process(envelope.container);

        // then
        verifyNoInteractions(leaseAcquirer);
        verifyNoInteractions(blobProcessor);
    }


    @Test
    void should_skip_blob_if_lease_cannot_be_acquired() {
        // given
        var envelope = envelope(Status.CREATED);
        storageHasBlob(envelope.fileName, envelope.container);
        leaseCannotBeAcquired();

        // when
        containerProcessor.process(envelope.container);

        // then
        verifyNoInteractions(blobProcessor);
        verify(envelopeService).findEnvelopeNotInCreatedStatus(envelope.fileName, envelope.container);
        verifyNoMoreInteractions(envelopeService);
    }

    private void storageHasBlob(String fileName, String containerName) {
        given(storageClient.getBlobContainerClient(containerName)).willReturn(containerClient);
        given(containerClient.listBlobs()).willReturn(listBlobsResult);

        var blob = blob(fileName);
        given(listBlobsResult.stream()).willReturn(Stream.of(blob));
        given(containerClient.getBlobClient(blob.getName())).willReturn(blobClient);
        given(blobClient.getBlobName()).willReturn(fileName);
        given(blobClient.getContainerName()).willReturn(containerName);
    }

    private void dbHas(Envelope envelope) {
        given(envelopeService.findEnvelopeNotInCreatedStatus(envelope.fileName, envelope.container))
            .willReturn(Optional.of(envelope));
    }

    private void envelopeStatusChangedInDb(Envelope envelope, Status status) {
        Envelope envelopeInNewStatus = envelope(envelope.id, status);
        given(envelopeService.findLastEnvelope(envelope.fileName, envelope.container))
            .willReturn(Optional.of(envelope)).willReturn(Optional.of(envelopeInNewStatus));
    }

    private void envelopeDeletedFromDb(Envelope envelope) {
        given(envelopeService.findLastEnvelope(envelope.fileName, envelope.container))
            .willReturn(Optional.of(envelope)).willReturn(Optional.empty());
    }

    @SuppressWarnings("unchecked")
    private void leaseCanBeAcquired() {
        doAnswer(invocation -> {
            var okAction = (Consumer) invocation.getArgument(1);
            okAction.accept(UUID.randomUUID().toString());
            return null;
        }).when(leaseAcquirer).ifAcquiredOrElse(any(), any(), any(), anyBoolean());
    }

    @SuppressWarnings("unchecked")
    private void leaseCannotBeAcquired() {
        doAnswer(invocation -> {
            var failureAction = (Consumer) invocation.getArgument(2);
            failureAction.accept(BlobErrorCode.INVALID_INPUT);
            return null;
        }).when(leaseAcquirer).ifAcquiredOrElse(any(), any(), any(), anyBoolean());
    }

    private Envelope envelope(Status status) {
        return envelope(UUID.randomUUID(), status);
    }

    private Envelope envelope(UUID id, Status status) {
        return new Envelope(
            id,
            "some_container",
            "hello.zip",
            now(),
            null,
            null,
            status,
            false,
            false
        );
    }

    private BlobItem blob(String name) {
        var blobItem = mock(BlobItem.class);
        given(blobItem.getName()).willReturn(name);
        return blobItem;
    }
}
