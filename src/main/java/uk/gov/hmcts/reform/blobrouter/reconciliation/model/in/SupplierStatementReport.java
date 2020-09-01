package uk.gov.hmcts.reform.blobrouter.reconciliation.model.in;

import com.fasterxml.jackson.annotation.JsonProperty;

public class SupplierStatementReport {

    public final SupplierStatement supplierStatement;

    public SupplierStatementReport(
        @JsonProperty(value = "report", required = true) SupplierStatement supplierStatement
    ) {
        this.supplierStatement = supplierStatement;
    }
}
