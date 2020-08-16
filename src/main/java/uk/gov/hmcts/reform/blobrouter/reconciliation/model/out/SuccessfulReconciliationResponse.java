package uk.gov.hmcts.reform.blobrouter.reconciliation.model.out;

import com.fasterxml.jackson.annotation.JsonProperty;

public class SuccessfulReconciliationResponse {

    @JsonProperty("id")
    public final String id;

    public SuccessfulReconciliationResponse(String id) {
        this.id = id;
    }
}
