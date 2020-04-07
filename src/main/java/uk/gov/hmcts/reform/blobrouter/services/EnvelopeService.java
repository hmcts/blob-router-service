package uk.gov.hmcts.reform.blobrouter.services;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import reactor.util.function.Tuple2;
import reactor.util.function.Tuples;
import uk.gov.hmcts.reform.blobrouter.data.envelopes.Envelope;
import uk.gov.hmcts.reform.blobrouter.data.envelopes.EnvelopeRepository;
import uk.gov.hmcts.reform.blobrouter.data.envelopes.NewEnvelope;
import uk.gov.hmcts.reform.blobrouter.data.envelopes.Status;
import uk.gov.hmcts.reform.blobrouter.data.events.EnvelopeEvent;
import uk.gov.hmcts.reform.blobrouter.data.events.EnvelopeEventRepository;
import uk.gov.hmcts.reform.blobrouter.data.events.EventType;
import uk.gov.hmcts.reform.blobrouter.data.events.NewEnvelopeEvent;
import uk.gov.hmcts.reform.blobrouter.exceptions.EnvelopeNotFoundException;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static java.time.Instant.now;
import static java.util.stream.Collectors.toList;

@Service
public class EnvelopeService {

    private final EnvelopeRepository envelopeRepository;
    private final EnvelopeEventRepository eventRepository;

    public EnvelopeService(
        EnvelopeRepository envelopeRepository,
        EnvelopeEventRepository eventRepository
    ) {
        this.envelopeRepository = envelopeRepository;
        this.eventRepository = eventRepository;
    }

    @Transactional(readOnly = true)
    public Optional<Envelope> findLastEnvelope(String blobName, String containerName) {
        return envelopeRepository.findLast(blobName, containerName);
    }

    @Transactional(readOnly = true)
    public Optional<Envelope> findEnvelope(UUID id) {
        return envelopeRepository.find(id);
    }

    @Transactional
    public UUID createNewEnvelope(String containerName, String blobName, Instant blobCreationDate) {
        UUID id = envelopeRepository
            .insert(
                new NewEnvelope(containerName, blobName, blobCreationDate, null, Status.CREATED)
            );

        eventRepository.insert(new NewEnvelopeEvent(id, EventType.FILE_PROCESSING_STARTED, null, null));

        return id;
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
                    envelopeRepository.updateDispatchDateTime(id, now());
                    eventRepository.insert(new NewEnvelopeEvent(id, EventType.DISPATCHED, null, null));
                },
                () -> {
                    throw new EnvelopeNotFoundException("Envelope with ID: " + id + " not found");
                }
            );
    }

    @Transactional
    public void markAsRejected(UUID id, String reason) {
        envelopeRepository
            .find(id)
            .ifPresentOrElse(
                env -> {
                    envelopeRepository.updateStatus(id, Status.REJECTED);
                    envelopeRepository.updatePendingNotification(id, true); // notification pending
                    // TODO: save error_code
                    eventRepository.insert(new NewEnvelopeEvent(id, EventType.REJECTED, null, reason));
                },
                () -> {
                    throw new EnvelopeNotFoundException("Envelope with ID: " + id + " not found");
                }
            );
    }

    @Transactional
    public void markPendingNotificationAsSent(UUID id) {
        envelopeRepository.updatePendingNotification(id, false);
        eventRepository.insert(new NewEnvelopeEvent(id, EventType.NOTIFICATION_SENT, null, null));
    }

    @Transactional
    public void markEnvelopeAsDeleted(Envelope envelope) {
        envelopeRepository.markAsDeleted(envelope.id);
        eventRepository.insert(new NewEnvelopeEvent(envelope.id, EventType.DELETED, null, null));
    }

    @Transactional
    public void saveEvent(UUID envelopeId, EventType eventType) {
        eventRepository.insert(new NewEnvelopeEvent(envelopeId, eventType, null, null));
    }

    @Transactional(readOnly = true)
    public List<Tuple2<Envelope, List<EnvelopeEvent>>> getEnvelopeInfo(String blobName, String containerName) {
        return envelopeRepository
            .find(blobName, containerName)
            .stream()
            .map(envelope -> Tuples.of(
                envelope,
                eventRepository.findForEnvelope(envelope.id)
            ))
            .collect(toList());
    }
}
