package uk.gov.hmcts.reform.blobrouter.services;

import com.google.common.collect.ImmutableList;
import org.apache.commons.lang3.StringUtils;
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
import uk.gov.hmcts.reform.blobrouter.data.events.ErrorCode;
import uk.gov.hmcts.reform.blobrouter.data.events.EventType;
import uk.gov.hmcts.reform.blobrouter.data.events.NewEnvelopeEvent;
import uk.gov.hmcts.reform.blobrouter.exceptions.EnvelopeNotFoundException;
import uk.gov.hmcts.reform.blobrouter.exceptions.InvalidRequestParametersException;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static java.time.Instant.now;
import static java.util.Collections.emptyList;
import static java.util.stream.Collectors.groupingBy;
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
    public Optional<Envelope> findEnvelopeNotInCreatedStatus(String fileName, String containerName) {
        return envelopeRepository.findEnvelopeNotInCreatedStatus(fileName, containerName);
    }

    @Transactional(readOnly = true)
    public Optional<Envelope> findEnvelope(UUID id) {
        return envelopeRepository.find(id);
    }

    @Transactional
    public UUID createNewEnvelope(String containerName, String blobName, Instant blobCreationDate) {
        UUID id = envelopeRepository
            .insert(
                new NewEnvelope(containerName, blobName, blobCreationDate, null, Status.CREATED, null)
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
    public void markAsRejected(UUID id, ErrorCode errorCode, String reason) {
        envelopeRepository
            .find(id)
            .ifPresentOrElse(
                env -> {
                    envelopeRepository.updateStatus(id, Status.REJECTED);
                    envelopeRepository.updatePendingNotification(id, true); // notification pending
                    eventRepository.insert(new NewEnvelopeEvent(id, EventType.REJECTED, errorCode, reason));
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
    public void saveEvent(UUID envelopeId, EventType eventType, String notes) {
        eventRepository.insert(new NewEnvelopeEvent(envelopeId, eventType, null, notes));
    }

    @Transactional
    public void saveEvent(UUID envelopeId, EventType eventType) {
        eventRepository.insert(new NewEnvelopeEvent(envelopeId, eventType, null, null));
    }

    @Transactional(readOnly = true)
    public List<Tuple2<Envelope, List<EnvelopeEvent>>> getEnvelopes(
        String blobName,
        String containerName,
        LocalDate date
    ) {
        if (StringUtils.isEmpty(blobName) && date == null) {
            throw new InvalidRequestParametersException("'file_name' or 'date' must not be null or empty");
        }

        List<Envelope> envelopes = envelopeRepository
            .findEnvelopes(blobName, containerName, date);

        if (envelopes.isEmpty()) {
            return emptyList();
        } else {
            List<UUID> envelopeIds = envelopes.stream().map(e -> e.id).collect(toList());

            List<EnvelopeEvent> envelopeEvents = eventRepository.findForEnvelopes(envelopeIds);

            Map<UUID, List<EnvelopeEvent>> eventsByEnvelopeIds = envelopeEvents
                .stream()
                .collect(groupingBy(envelopeEvent -> envelopeEvent.envelopeId));

            return envelopes
                .stream()
                .map(envelope -> Tuples.of(
                    envelope,
                    getEnvelopeEvents(eventsByEnvelopeIds, envelope)
                ))
                .collect(toList());
        }
    }

    @Transactional(readOnly = true)
    public List<Envelope> getEnvelopes(LocalDate date) {
        var envelopes = envelopeRepository.findEnvelopes(null, null, date);
        return envelopes.isEmpty() ? emptyList() : ImmutableList.copyOf(envelopes);
    }

    @Transactional(readOnly = true)
    public List<Envelope> getEnvelopesByDcnPrefix(String dcnPrefix, LocalDate fromDate, LocalDate toDate) {
        var envelopes = envelopeRepository.findEnvelopesByDcnPrefix(dcnPrefix, fromDate, toDate);
        return envelopes.isEmpty() ? emptyList() : ImmutableList.copyOf(envelopes);
    }

    private List<EnvelopeEvent> getEnvelopeEvents(
        Map<UUID, List<EnvelopeEvent>> eventsByEnvelopeIds,
        Envelope envelope
    ) {
        List<EnvelopeEvent> envelopeEvents = eventsByEnvelopeIds.get(envelope.id);
        return envelopeEvents == null ? emptyList() : envelopeEvents;
    }
}
