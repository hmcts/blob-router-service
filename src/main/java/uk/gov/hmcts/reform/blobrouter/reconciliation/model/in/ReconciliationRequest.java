package uk.gov.hmcts.reform.blobrouter.reconciliation.model.in;

import com.fasterxml.jackson.annotation.JsonProperty;

public class ReconciliationRequest {

    public final Report report;

    public ReconciliationRequest(@JsonProperty(value = "report") Report report) {
        this.report = report;
    }
}
