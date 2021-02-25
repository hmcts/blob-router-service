package uk.gov.hmcts.reform.blobrouter.services;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uk.gov.hmcts.reform.blobrouter.clients.bulkscanprocessor.BulkScanProcessorClient;
import uk.gov.hmcts.reform.blobrouter.clients.response.ZipFileResponse;
import uk.gov.hmcts.reform.blobrouter.data.envelopes.Envelope;
import uk.gov.hmcts.reform.blobrouter.data.envelopes.EnvelopeRepository;
import uk.gov.hmcts.reform.blobrouter.data.envelopes.Status;
import uk.gov.hmcts.reform.blobrouter.data.events.EnvelopeEventRepository;
import uk.gov.hmcts.reform.blobrouter.data.events.EventType;
import uk.gov.hmcts.reform.blobrouter.data.events.NewEnvelopeEvent;
import uk.gov.hmcts.reform.blobrouter.exceptions.EnvelopeCompletedOrNotStaleException;
import uk.gov.hmcts.reform.blobrouter.exceptions.EnvelopeNotFoundException;

import java.time.Instant;
import java.util.UUID;

import static java.time.Duration.between;
import static java.time.Instant.now;
import static java.util.Comparator.naturalOrder;
import static uk.gov.hmcts.reform.blobrouter.data.envelopes.Status.DISPATCHED;
import static uk.gov.hmcts.reform.blobrouter.data.envelopes.Status.REJECTED;
import static uk.gov.hmcts.reform.blobrouter.data.events.ErrorCode.ERR_STALE_ENVELOPE;
import static uk.gov.hmcts.reform.blobrouter.data.events.EventType.MANUALLY_MARKED_AS_DISPATCHED;
import static uk.gov.hmcts.reform.blobrouter.data.events.EventType.MANUALLY_MARKED_AS_REJECTED;

@Service
public class EnvelopeActionService {

    private final EnvelopeRepository envelopeRepository;
    private final EnvelopeEventRepository envelopeEventRepository;
    private final BulkScanProcessorClient bulkScanProcessorClient;
    private final long envelopeStaleTimeoutHr;

    public EnvelopeActionService(
        EnvelopeRepository envelopeRepository,
        EnvelopeEventRepository envelopeEventRepository,
        BulkScanProcessorClient bulkScanProcessorClient,
        @Value("${envelope-stale-timeout-hr}") long envelopeStaleTimeoutHr
    ) {
        this.envelopeRepository = envelopeRepository;
        this.envelopeEventRepository = envelopeEventRepository;
        this.bulkScanProcessorClient = bulkScanProcessorClient;
        this.envelopeStaleTimeoutHr = envelopeStaleTimeoutHr;
    }

    @Transactional
    public void completeStaleEnvelope(UUID envelopeId) {
        Envelope envelope = envelopeRepository.find(envelopeId)
            .orElseThrow(
                () -> new EnvelopeNotFoundException("Envelope with id " + envelopeId + " not found")
            );
        validateEnvelopeState(envelope);

        ZipFileResponse response = bulkScanProcessorClient.getZipFile(envelope.fileName);
        if (isZipFileUnknownToProcessor(response)) {
            moveEnvelopeToRejected(envelopeId);
        } else {
            moveEnvelopeToDispatched(envelopeId);
        }
    }

    private void moveEnvelopeToRejected(UUID envelopeId) {
        moveEnvelopeToFinalState(
            envelopeId,
            REJECTED,
            MANUALLY_MARKED_AS_REJECTED,
            "Manually marked as rejected due to stale state"
        );
    }

    private void moveEnvelopeToDispatched(UUID envelopeId) {
        moveEnvelopeToFinalState(
            envelopeId,
            DISPATCHED,
            MANUALLY_MARKED_AS_DISPATCHED,
            "Manually marked as dispatched due to stale state"
        );
    }

    private void moveEnvelopeToFinalState(
        UUID envelopeId,
        Status status,
        EventType eventType,
        String message
    ) {
        createEvent(
            envelopeId,
            eventType,
            message
        );
        envelopeRepository.updateStatus(envelopeId, status);
    }

    private boolean isZipFileUnknownToProcessor(ZipFileResponse response) {
        return response.envelopes.isEmpty() && response.events.isEmpty();
    }

    private void createEvent(
        UUID envelopeId,
        EventType eventType,
        String message
    ) {
        NewEnvelopeEvent event = new NewEnvelopeEvent(
            envelopeId,
            eventType,
            ERR_STALE_ENVELOPE,
            message
        );
        envelopeEventRepository.insert(event);
    }

    private void validateEnvelopeState(Envelope envelope) {
        if (envelope.status == DISPATCHED || envelope.status == REJECTED
            || !isStale(envelope)) {
            throw new EnvelopeCompletedOrNotStaleException(
                "Envelope with id " + envelope.id + " is completed or not stale"
            );
        }
    }

    private boolean isStale(Envelope envelope) {
        if (envelope.status != Status.CREATED) {
            return false;
        }

        Instant lastEventTimeStamp = envelopeEventRepository
            .findForEnvelope(envelope.id)
            .stream()
            .map(event -> event.createdAt)
            .max(naturalOrder())
            .orElseThrow(); // no events for the envelope is normally impossible
        return between(lastEventTimeStamp, now()).toHours() > envelopeStaleTimeoutHr;
    }
}
