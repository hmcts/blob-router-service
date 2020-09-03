package uk.gov.hmcts.reform.blobrouter.data.reconciliation.reports.model;

import java.util.UUID;

public class NewReconciliationReport {

    public final UUID supplierStatementId;
    public final String account;
    public final String summaryContent;
    public final String detailedContent;
    public final String contentTypeVersion;

    public NewReconciliationReport(
        UUID supplierStatementId,
        String account,
        String summaryContent,
        String detailedContent,
        String contentTypeVersion
    ) {
        this.supplierStatementId = supplierStatementId;
        this.account = account;
        this.summaryContent = summaryContent;
        this.detailedContent = detailedContent;
        this.contentTypeVersion = contentTypeVersion;
    }
}
