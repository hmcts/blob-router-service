package uk.gov.hmcts.reform.blobrouter.data.events;

import uk.gov.hmcts.reform.blobrouter.data.events.EventType;

import java.time.Instant;
import java.util.UUID;

public class EnvelopeEvent {

    public final long id;
    public final UUID envelopeId;
    public final EventType type;
    public final String notes;
    public final Instant createdAt;

    public EnvelopeEvent(long id, UUID envelopeId, EventType type, String notes, Instant createdAt) {
        this.id = id;
        this.envelopeId = envelopeId;
        this.type = type;
        this.notes = notes;
        this.createdAt = createdAt;
    }
}
