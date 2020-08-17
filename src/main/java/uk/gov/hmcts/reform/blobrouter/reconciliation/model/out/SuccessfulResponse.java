package uk.gov.hmcts.reform.blobrouter.reconciliation.model.out;

import com.fasterxml.jackson.annotation.JsonProperty;

public class SuccessfulResponse {

    @JsonProperty("id")
    public final String id;

    public SuccessfulResponse(String id) {
        this.id = id;
    }
}
