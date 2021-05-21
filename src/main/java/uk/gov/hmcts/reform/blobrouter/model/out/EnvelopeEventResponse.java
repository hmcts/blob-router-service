package uk.gov.hmcts.reform.blobrouter.model.out;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import uk.gov.hmcts.reform.blobrouter.util.InstantSerializer;

import java.time.Instant;

public class EnvelopeEventResponse {

    @JsonProperty("id")
    public final long id;

    @JsonProperty("created_at")
    @JsonSerialize(using = InstantSerializer.class)
    public final Instant createdAt;

    @JsonProperty("event")
    public final String event;

    @JsonProperty("notes")
    public final String notes;

    public EnvelopeEventResponse(long id, Instant createdAt, String event, String notes) {
        this.id = id;
        this.createdAt = createdAt;
        this.event = event;
        this.notes = notes;
    }
}
