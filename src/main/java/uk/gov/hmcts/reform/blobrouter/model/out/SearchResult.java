package uk.gov.hmcts.reform.blobrouter.model.out;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public class SearchResult {

    @JsonProperty("count")
    public final int count;

    @JsonProperty("data")
    public final List<?> data;

    public SearchResult(List<?> data) {
        this.data = data;
        count = (data == null) ? 0 : data.size();
    }
}
