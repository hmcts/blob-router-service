package uk.gov.hmcts.reform.blobrouter.data.reconciliation.model;

import java.util.UUID;

public class NewReconciliationReport {

    public final UUID supplierStatementId;
    public final String account;
    public final String content;
    public final String contentTypeVersion;

    public NewReconciliationReport(
        UUID supplierStatementId,
        String account,
        String content,
        String contentTypeVersion
    ) {
        this.supplierStatementId = supplierStatementId;
        this.account = account;
        this.content = content;
        this.contentTypeVersion = contentTypeVersion;
    }
}
