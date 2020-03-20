package uk.gov.hmcts.reform.blobrouter.data.events;

import java.util.UUID;

public class NewEnvelopeEvent {

    public final UUID envelopeId;
    public final EventType type;
    public final String notes;

    public NewEnvelopeEvent(UUID envelopeId, EventType type, String notes) {
        this.envelopeId = envelopeId;
        this.type = type;
        this.notes = notes;
    }
}
