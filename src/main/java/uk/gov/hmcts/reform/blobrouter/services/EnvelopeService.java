package uk.gov.hmcts.reform.blobrouter.services;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import reactor.util.function.Tuple2;
import reactor.util.function.Tuples;
import uk.gov.hmcts.reform.blobrouter.data.EnvelopeRepository;
import uk.gov.hmcts.reform.blobrouter.data.EventRecordRepository;
import uk.gov.hmcts.reform.blobrouter.data.model.Envelope;
import uk.gov.hmcts.reform.blobrouter.data.model.Event;
import uk.gov.hmcts.reform.blobrouter.data.model.EventRecord;
import uk.gov.hmcts.reform.blobrouter.data.model.NewEnvelope;
import uk.gov.hmcts.reform.blobrouter.data.model.NewEventRecord;
import uk.gov.hmcts.reform.blobrouter.data.model.Status;
import uk.gov.hmcts.reform.blobrouter.exceptions.EnvelopeNotFoundException;

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
    public UUID createNewEnvelope(String containerName, String blobName, Instant blobCreationDate) {
        UUID id = envelopeRepository
            .insert(
                new NewEnvelope(containerName, blobName, blobCreationDate, null, Status.CREATED)
            );

        eventRecordRepository.insert(new NewEventRecord(containerName, blobName, Event.FILE_PROCESSING_STARTED));

        return id;
    }

    @Transactional
    public UUID createDispatchedEnvelope(String containerName, String blobName, Instant blobCreationDate) {
        eventRecordRepository.insert(new NewEventRecord(containerName, blobName, Event.DISPATCHED));

        return envelopeRepository
            .insert(
                new NewEnvelope(containerName, blobName, blobCreationDate, now(), Status.DISPATCHED)
            );
    }

    @Transactional
    public UUID createRejectedEnvelope(
        String containerName,
        String blobName,
        Instant blobCreationDate,
        String rejectionReason
    ) {
        eventRecordRepository.insert(new NewEventRecord(containerName, blobName, Event.REJECTED, rejectionReason));

        return envelopeRepository
            .insert(
                new NewEnvelope(containerName, blobName, blobCreationDate, null, Status.REJECTED)
            );
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
    public void markAsDispatched(UUID id) {
        envelopeRepository
            .find(id)
            .ifPresentOrElse(
                env -> {
                    envelopeRepository.updateStatus(id, Status.DISPATCHED);
                    eventRecordRepository.insert(new NewEventRecord(env.container, env.fileName, Event.DISPATCHED));
                },
                () -> {
                    throw new EnvelopeNotFoundException("Envelope with ID: " + id + " not found");
                }
            );
    }

    @Transactional
    public void markEnvelopeAsDeleted(Envelope envelope) {
        envelopeRepository.markAsDeleted(envelope.id);
        eventRecordRepository.insert(new NewEventRecord(envelope.container, envelope.fileName, Event.DELETED));
    }

    @Transactional
    public void saveEventFileProcessingStarted(String containerName, String blobName) {
        eventRecordRepository.insert(new NewEventRecord(containerName, blobName, Event.FILE_PROCESSING_STARTED));
    }

    @Transactional
    public void saveEventDeletedFromRejected(String containerName, String blobName) {
        eventRecordRepository.insert(new NewEventRecord(containerName, blobName, Event.DELETED_FROM_REJECTED));
    }

    @Transactional
    public void saveEventDuplicateRejected(String containerName, String blobName) {
        eventRecordRepository.insert(new NewEventRecord(containerName, blobName, Event.DUPLICATE_REJECTED));
    }

    @Transactional
    public void saveEventError(String containerName, String blobName) {
        eventRecordRepository.insert(new NewEventRecord(containerName, blobName, Event.ERROR));
    }

    @Transactional(readOnly = true)
    public Optional<Tuple2<Envelope, List<EventRecord>>> getEnvelopeInfo(String blobName, String containerName) {
        return findEnvelope(blobName, containerName)
            .map(envelope -> Tuples.of(
                envelope,
                eventRecordRepository.find(containerName, blobName)
            ));
    }
}
