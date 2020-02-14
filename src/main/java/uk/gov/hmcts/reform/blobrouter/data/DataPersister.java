package uk.gov.hmcts.reform.blobrouter.data;

import org.springframework.transaction.annotation.Transactional;
import uk.gov.hmcts.reform.blobrouter.data.model.Envelope;
import uk.gov.hmcts.reform.blobrouter.data.model.NewEnvelope;
import uk.gov.hmcts.reform.blobrouter.data.model.Status;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static java.time.Instant.now;

public class DataPersister {

    private final EnvelopeRepository envelopeRepository;
    private final EventRecordRepository eventRecordRepository;

    public DataPersister(
        EnvelopeRepository envelopeRepository,
        EventRecordRepository eventRecordRepository
    ) {
        this.envelopeRepository = envelopeRepository;
        this.eventRecordRepository = eventRecordRepository;
    }

    // region BlobProcessor actions

    @Transactional(readOnly = true)
    public Optional<Envelope> findEnvelopes(String blobName, String containerName) {
        return envelopeRepository.find(blobName, containerName);
    }

    @Transactional
    public UUID createDispatchedEnvelope(String containerName, String blobName, Instant blobCreationDate) {
        return createEnvelope(containerName, blobName, blobCreationDate, Status.DISPATCHED);
    }

    @Transactional
    public UUID createRejectedEnvelope(String containerName, String blobName, Instant blobCreationDate) {
        return createEnvelope(containerName, blobName, blobCreationDate, Status.REJECTED);
    }

    private UUID createEnvelope(String containerName, String blobName, Instant blobCreationDate, Status status) {
        return envelopeRepository.insert(new NewEnvelope(
            containerName,
            blobName,
            blobCreationDate,
            now(),
            status
        ));
    }

    // endregion

    // region RejectedFilesHandler actions

    @Transactional(readOnly = true)
    public List<Envelope> getReadyToDeleteRejections() {
        return envelopeRepository.find(Status.REJECTED, false);
    }

    // endregion

    // region Container cleaner actions

    @Transactional(readOnly = true)
    public List<Envelope> getReadyToDeleteDispatches(String containerName) {
        return envelopeRepository.find(Status.DISPATCHED, containerName, false);
    }

    // endregion

    // region common for RejectedFilesHandler and ContainerCleaner

    @Transactional
    public void markEnvelopeAsDeleted(UUID envelopeId) {
        envelopeRepository.markAsDeleted(envelopeId);
    }

    // endregion
}
