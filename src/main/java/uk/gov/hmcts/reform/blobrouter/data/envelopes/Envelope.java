package uk.gov.hmcts.reform.blobrouter.data.envelopes;

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
    public final boolean pendingNotification;
    public final Long fileSize;

    public Envelope(
        UUID id,
        String container,
        String fileName,
        Instant createdAt,
        Instant fileCreatedAt,
        Instant dispatchedAt,
        Status status,
        boolean isDeleted,
        boolean pendingNotification,
        Long fileSize
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
        this.fileSize = fileSize;
    }

    public String getBasicInfo() {
        return String.format(
            "Envelope ID: %s, File name: %s, Container: %s, Status: %s",
            this.id,
            this.fileName,
            this.container,
            this.status
        );
    }
}
