package uk.gov.hmcts.reform.blobrouter.model.out;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public class SearchResult {

    @JsonProperty("data")
    public final List<?> data;

    public SearchResult(List<?> data) {
        this.data = data;
    }
}
