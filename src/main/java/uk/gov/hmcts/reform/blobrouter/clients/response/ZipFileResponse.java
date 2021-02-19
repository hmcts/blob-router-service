package uk.gov.hmcts.reform.blobrouter.clients.response;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public class ZipFileResponse {

    public final String fileName;
    public final List<Object> envelopes;
    public final List<Object> events;

    public ZipFileResponse(
        @JsonProperty("file_name") String fileName,
        @JsonProperty("envelopes") List<Object> envelopes,
        @JsonProperty("events") List<Object> events
    ) {
        this.fileName = fileName;
        this.envelopes = envelopes;
        this.events = events;
    }
}
