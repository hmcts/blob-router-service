package uk.gov.hmcts.reform.blobrouter.data.model;

import java.time.Instant;

public class EnvelopeSummary {

    private final String container;
    private final String fileName;
    private final Instant fileCreatedAt;
    private final Instant dispatchedAt;
    private final Status status;
    private final boolean isDeleted;

    public EnvelopeSummary(
        String container,
        String fileName,
        Instant fileCreatedAt,
        Instant dispatchedAt,
        Status status,
        boolean isDeleted
    ) {
        this.container = container;
        this.fileName = fileName;
        this.fileCreatedAt = fileCreatedAt;
        this.dispatchedAt = dispatchedAt;
        this.status = status;
        this.isDeleted = isDeleted;
    }
}
