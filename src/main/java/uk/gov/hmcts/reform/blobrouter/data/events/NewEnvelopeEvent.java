package uk.gov.hmcts.reform.blobrouter.data.events;

import java.util.UUID;

public class NewEnvelopeEvent {

    public final UUID envelopeId;
    public final EventType type;
    public final ErrorCode errorCode;
    public final String notes;

    public NewEnvelopeEvent(UUID envelopeId, EventType type, ErrorCode errorCode, String notes) {
        this.envelopeId = envelopeId;
        this.type = type;
        this.errorCode = errorCode;
        this.notes = notes;
    }
}
