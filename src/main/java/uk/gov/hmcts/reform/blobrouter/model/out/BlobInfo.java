package uk.gov.hmcts.reform.blobrouter.model.out;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import uk.gov.hmcts.reform.blobrouter.util.InstantSerializer;

import java.time.Instant;

public class BlobInfo {

    @JsonProperty("container")
    public final String container;

    @JsonProperty("file_name")
    public final String fileName;

    @JsonProperty("created_at")
    @JsonSerialize(using = InstantSerializer.class)
    public final Instant createdAt;

    public BlobInfo(
        String container,
        String fileName,
        Instant createdAt
    ) {
        this.container = container;
        this.fileName = fileName;
        this.createdAt = createdAt;
    }
}
