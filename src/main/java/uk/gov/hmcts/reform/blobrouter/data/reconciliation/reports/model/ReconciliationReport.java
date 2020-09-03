package uk.gov.hmcts.reform.blobrouter.data.reconciliation.reports.model;

import java.time.LocalDateTime;
import java.util.UUID;

public class ReconciliationReport {

    public final UUID id;
    public final UUID supplierStatementId;
    public final String account;
    public final String summaryContent;
    public final String detailedContent;
    public final String contentTypeVersion;
    public final LocalDateTime sentAt;
    public final LocalDateTime createdAt;

    public ReconciliationReport(
        UUID id,
        UUID supplierStatementId,
        String account,
        String summaryContent,
        String detailedContent,
        String contentTypeVersion,
        LocalDateTime sentAt,
        LocalDateTime createdAt
    ) {
        this.id = id;
        this.supplierStatementId = supplierStatementId;
        this.account = account;
        this.summaryContent = summaryContent;
        this.detailedContent = detailedContent;
        this.contentTypeVersion = contentTypeVersion;
        this.sentAt = sentAt;
        this.createdAt = createdAt;
    }
}
