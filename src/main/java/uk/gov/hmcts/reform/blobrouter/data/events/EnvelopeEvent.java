package uk.gov.hmcts.reform.blobrouter.data.events;

import java.time.Instant;
import java.util.UUID;

/**
 * The EnvelopeEvent class represents an event associated with an envelope, storing information such as ID, envelope ID,
 * event type, error code, notes, and creation timestamp.
 */
public class EnvelopeEvent {

    public final long id;
    public final UUID envelopeId;
    public final EventType type;
    public final ErrorCode errorCode;
    public final String notes;
    public final Instant createdAt;

    public EnvelopeEvent(
        long id,
        UUID envelopeId,
        EventType type,
        ErrorCode errorCode,
        String notes,
        Instant createdAt
    ) {
        this.id = id;
        this.envelopeId = envelopeId;
        this.type = type;
        this.errorCode = errorCode;
        this.notes = notes;
        this.createdAt = createdAt;
    }
}
