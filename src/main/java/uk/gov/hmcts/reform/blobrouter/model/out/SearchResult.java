package uk.gov.hmcts.reform.blobrouter.model.out;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public class SearchResult {

    @JsonProperty("data")
    public final List<EnvelopeInfo> data;

    public SearchResult(List<EnvelopeInfo> data) {
        this.data = data;
    }
}
