package uk.gov.hmcts.reform.blobrouter.model.out;

import com.fasterxml.jackson.annotation.JsonProperty;

public class BlobInfo {

    @JsonProperty("container")
    public final String container;

    @JsonProperty("file_name")
    public final String fileName;

    @JsonProperty("envelope_id")
    public final String envelopeId;

    @JsonProperty("created_at")
    public final String createdAt;

    public BlobInfo(
        String container,
        String fileName,
        String envelopeId,
        String createdAt
    ) {
        this.container = container;
        this.fileName = fileName;
        this.envelopeId = envelopeId;
        this.createdAt = createdAt;
    }
}
