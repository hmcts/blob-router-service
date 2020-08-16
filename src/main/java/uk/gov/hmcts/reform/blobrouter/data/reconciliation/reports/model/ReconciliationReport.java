package uk.gov.hmcts.reform.blobrouter.data.reconciliation.reports.model;

import java.time.LocalDateTime;
import java.util.UUID;

public class ReconciliationReport {

    public final UUID id;
    public final UUID supplierStatementId;
    public final String account;
    public final String content;
    public final String contentTypeVersion;
    public final LocalDateTime sentAt;
    public final LocalDateTime createdAt;

    public ReconciliationReport(
        UUID id,
        UUID supplierStatementId,
        String account,
        String content,
        String contentTypeVersion,
        LocalDateTime sentAt,
        LocalDateTime createdAt
    ) {
        this.id = id;
        this.supplierStatementId = supplierStatementId;
        this.account = account;
        this.content = content;
        this.contentTypeVersion = contentTypeVersion;
        this.sentAt = sentAt;
        this.createdAt = createdAt;
    }
}
