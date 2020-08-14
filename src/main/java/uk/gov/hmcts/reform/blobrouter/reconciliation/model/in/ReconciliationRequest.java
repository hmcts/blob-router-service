package uk.gov.hmcts.reform.blobrouter.reconciliation.model.in;

import com.fasterxml.jackson.annotation.JsonProperty;

public class ReconciliationRequest {

    public final SupplierStatement supplierStatement;

    public ReconciliationRequest(
        @JsonProperty(value = "report", required = true) SupplierStatement supplierStatement
    ) {
        this.supplierStatement = supplierStatement;
    }
}
