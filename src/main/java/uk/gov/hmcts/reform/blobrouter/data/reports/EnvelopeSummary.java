package uk.gov.hmcts.reform.blobrouter.data.reports;

import uk.gov.hmcts.reform.blobrouter.data.envelopes.Status;

import java.time.Instant;

public class EnvelopeSummary {

    public final String container;
    public final String fileName;
    public final Instant fileCreatedAt;
    public final Instant dispatchedAt;
    public final Status status;
    public final boolean isDeleted;

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
