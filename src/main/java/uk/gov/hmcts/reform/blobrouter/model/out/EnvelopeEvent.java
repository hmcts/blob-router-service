package uk.gov.hmcts.reform.blobrouter.model.out;

import com.fasterxml.jackson.annotation.JsonProperty;
import uk.gov.hmcts.reform.blobrouter.data.model.EventRecord;

import java.time.Instant;

public class EnvelopeEvent {

    @JsonProperty("id")
    public final long id;

    @JsonProperty("created_at")
    public final Instant createdAt;

    @JsonProperty("event")
    public final String event;

    @JsonProperty("notes")
    public final String notes;

    public EnvelopeEvent(EventRecord eventRecord) {
        id = eventRecord.id;
        createdAt = eventRecord.createdAt;
        event = eventRecord.event.name();
        notes = eventRecord.notes;
    }
}
