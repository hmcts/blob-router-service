package uk.gov.hmcts.reform.blobrouter.data.model;

import java.time.Instant;
import java.util.UUID;

public class Envelope {

    public final UUID id;
    public final String container;
    public final String fileName;
    public final Instant createdAt;
    public final Instant fileCreatedAt;
    public final Instant dispatchedAt;
    public final Status status;
    public final boolean isDeleted;

    public Envelope(
        UUID id,
        String container,
        String fileName,
        Instant createdAt,
        Instant fileCreatedAt,
        Instant dispatchedAt,
        Status status,
        boolean isDeleted
    ) {
        this.id = id;
        this.container = container;
        this.fileName = fileName;
        this.createdAt = createdAt;
        this.fileCreatedAt = fileCreatedAt;
        this.dispatchedAt = dispatchedAt;
        this.status = status;
        this.isDeleted = isDeleted;
    }
}
