package uk.gov.hmcts.reform.blobrouter.data.envelopes;

import java.time.Instant;

/**
 * The `NewEnvelope` class represents an envelope containing file information with specific attributes
 * like container, filename, creation and dispatch timestamps, status, and file size.
 */
public class NewEnvelope {

    public final String container;
    public final String fileName;
    public final Instant fileCreatedAt;
    public final Instant dispatchedAt;
    public final Status status;
    public Long fileSize;

    public NewEnvelope(
            String container,
            String fileName,
            Instant fileCreatedAt,
            Instant dispatchedAt,
            Status status,
            Long fileSize
    ) {
        this.container = container;
        this.fileName = fileName;
        this.fileCreatedAt = fileCreatedAt;
        this.dispatchedAt = dispatchedAt;
        this.status = status;
        this.fileSize = fileSize;
    }
}
