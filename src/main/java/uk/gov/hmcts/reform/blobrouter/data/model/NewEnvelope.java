package uk.gov.hmcts.reform.blobrouter.data.model;

import java.time.Instant;

public class NewEnvelope {

    public final String container;
    public final String fileName;
    public final Instant fileCreatedAt;
    public final Instant dispatchedAt;
    public final Status status;

    public NewEnvelope(String container, String fileName, Instant fileCreatedAt, Instant dispatchedAt, Status status) {
        this.container = container;
        this.fileName = fileName;
        this.fileCreatedAt = fileCreatedAt;
        this.dispatchedAt = dispatchedAt;
        this.status = status;
    }
}
