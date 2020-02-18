package uk.gov.hmcts.reform.blobrouter.services;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uk.gov.hmcts.reform.blobrouter.data.EnvelopeRepository;
import uk.gov.hmcts.reform.blobrouter.data.EventRecordRepository;
import uk.gov.hmcts.reform.blobrouter.data.model.Envelope;
import uk.gov.hmcts.reform.blobrouter.data.model.Event;
import uk.gov.hmcts.reform.blobrouter.data.model.NewEnvelope;
import uk.gov.hmcts.reform.blobrouter.data.model.NewEventRecord;
import uk.gov.hmcts.reform.blobrouter.data.model.Status;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static java.time.Instant.now;

@Service
public class EnvelopeService {

    private final EnvelopeRepository envelopeRepository;
    private final EventRecordRepository eventRecordRepository;

    public EnvelopeService(
        EnvelopeRepository envelopeRepository,
        EventRecordRepository eventRecordRepository
    ) {
        this.envelopeRepository = envelopeRepository;
        this.eventRecordRepository = eventRecordRepository;
    }

    @Transactional(readOnly = true)
    public Optional<Envelope> findEnvelope(String blobName, String containerName) {
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

    @Transactional(readOnly = true)
    public List<Envelope> getReadyToDeleteRejections() {
        return envelopeRepository.find(Status.REJECTED, false);
    }

    @Transactional(readOnly = true)
    public List<Envelope> getReadyToDeleteDispatches(String containerName) {
        return envelopeRepository.find(Status.DISPATCHED, containerName, false);
    }

    @Transactional
    public void markEnvelopeAsDeleted(UUID envelopeId) {
        envelopeRepository.markAsDeleted(envelopeId);
    }

    @Transactional
    public void saveEventDuplicateRejected(String containerName, String blobName) {
        eventRecordRepository.insert(new NewEventRecord(containerName, blobName, Event.DUPLICATE_REJECTED));
    }
}
