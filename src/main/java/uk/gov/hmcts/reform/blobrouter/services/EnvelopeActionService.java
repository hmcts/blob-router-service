package uk.gov.hmcts.reform.blobrouter.services;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uk.gov.hmcts.reform.blobrouter.data.envelopes.Envelope;
import uk.gov.hmcts.reform.blobrouter.data.envelopes.EnvelopeRepository;
import uk.gov.hmcts.reform.blobrouter.data.envelopes.Status;
import uk.gov.hmcts.reform.blobrouter.data.events.EnvelopeEventRepository;
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
import static uk.gov.hmcts.reform.blobrouter.data.events.EventType.MANUALLY_REJECTED;

@Service
public class EnvelopeActionService {

    private final EnvelopeRepository envelopeRepository;
    private final EnvelopeEventRepository envelopeEventRepository;
    private final long creationTimeoutHr;

    public EnvelopeActionService(
        EnvelopeRepository envelopeRepository,
        EnvelopeEventRepository envelopeEventRepository,
        @Value("${creation-stale-timeout-hr}") long creationTimeoutHr
    ) {
        this.envelopeRepository = envelopeRepository;
        this.envelopeEventRepository = envelopeEventRepository;
        this.creationTimeoutHr = creationTimeoutHr;
    }

    @Transactional
    public void rejectStaleEnvelope(UUID envelopeId) {
        Envelope envelope = envelopeRepository.find(envelopeId)
            .orElseThrow(
                () -> new EnvelopeNotFoundException("Envelope with id " + envelopeId + " not found")
            );
        validateEnvelopeState(envelope);

        createEvent(envelope);

        envelopeRepository.updateStatus(envelopeId, REJECTED);
    }

    private void createEvent(Envelope envelope) {
        NewEnvelopeEvent event = new NewEnvelopeEvent(
            envelope.id,
            MANUALLY_REJECTED,
            ERR_STALE_ENVELOPE,
            "Manually rejected due to stale state"
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
        return between(lastEventTimeStamp, now()).toHours() > creationTimeoutHr;
    }
}
