package uk.gov.hmcts.reform.blobrouter.data.envelopes;

import java.time.Instant;

public class NewEnvelopeWithCreateDate {
    public final String container;
    public final String fileName;
    public final Instant fileCreatedAt;
    public final Instant dispatchedAt;
    public final Status status;
    public final Instant createdAt;

    public NewEnvelopeWithCreateDate(String container, String fileName, Instant fileCreatedAt,
                                     Instant dispatchedAt, Status status, Instant createdAt) {
        this.container = container;
        this.fileName = fileName;
        this.fileCreatedAt = fileCreatedAt;
        this.dispatchedAt = dispatchedAt;
        this.status = status;
        this.createdAt = createdAt;
    }
}
