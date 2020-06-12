package uk.gov.hmcts.reform.blobrouter.model.out;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.LocalDateTime;

public class EnvelopeEventResponse {

    @JsonProperty("id")
    public final long id;

    @JsonProperty("created_at")
    public final LocalDateTime createdAt;

    @JsonProperty("event")
    public final String event;

    @JsonProperty("notes")
    public final String notes;

    public EnvelopeEventResponse(long id, LocalDateTime createdAt, String event, String notes) {
        this.id = id;
        this.createdAt = createdAt;
        this.event = event;
        this.notes = notes;
    }
}
