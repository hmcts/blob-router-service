package uk.gov.hmcts.reform.blobrouter.data.envelopes;

import java.time.Instant;

public class TeamEnvelope {

    public final String etag;
    public final String fileName;
    public final String url;
    public final Instant createdAt;
    public final Long contentLength;
    public final String contentType;

    public TeamEnvelope(
        String etag,
        String fileName,
        String url,
        Instant createdAt,
        Long contentLength,
        String contentType
    ) {
        this.etag = etag;
        this.fileName = fileName;
        this.url = url;
        this.createdAt = createdAt;
        this.contentLength = contentLength;
        this.contentType = contentType;
    }
}
