package uk.gov.hmcts.reform.blobrouter.model.out;

import com.fasterxml.jackson.annotation.JsonProperty;
import uk.gov.hmcts.reform.blobrouter.data.envelopes.Status;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public class EnvelopeInfo {

    @JsonProperty("id")
    public final UUID id;

    @JsonProperty("container")
    public final String container;

    @JsonProperty("file_name")
    public final String fileName;

    @JsonProperty("created_at")
    public final Instant createdAt;

    @JsonProperty("file_created_at")
    public final Instant fileCreatedAt;

    @JsonProperty("dispatched_at")
    public final Instant dispatchedAt;

    @JsonProperty("status")
    public final Status status;

    @JsonProperty("is_deleted")
    public final boolean isDeleted;

    @JsonProperty("pending_notification")
    public final boolean pendingNotification;

    @JsonProperty("events")
    public final List<EnvelopeEventResponse> envelopeEvents;

    public EnvelopeInfo(
        UUID id,
        String container,
        String fileName,
        Instant createdAt,
        Instant fileCreatedAt,
        Instant dispatchedAt,
        Status status,
        boolean isDeleted,
        boolean pendingNotification,
        List<EnvelopeEventResponse> envelopeEvents
    ) {
        this.id = id;
        this.container = container;
        this.fileName = fileName;
        this.createdAt = createdAt;
        this.fileCreatedAt = fileCreatedAt;
        this.dispatchedAt = dispatchedAt;
        this.status = status;
        this.isDeleted = isDeleted;
        this.pendingNotification = pendingNotification;
        this.envelopeEvents = envelopeEvents;
    }
}
