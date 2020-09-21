package uk.gov.hmcts.reform.blobrouter.data.reconciliation.reports.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonRawValue;

import java.time.LocalDateTime;
import java.util.UUID;

public class ReconciliationReport {

    @JsonProperty("id")
    public final UUID id;

    @JsonProperty("supplier_statement_id")
    public final UUID supplierStatementId;

    @JsonProperty("account")
    public final String account;

    // Summary Content is Json value, serialize as it is
    @JsonRawValue
    @JsonProperty("summary_content")
    public final String summaryContent;

    // Detailed Content is Json value, serialize as it is
    @JsonRawValue
    @JsonProperty("detailed_content")
    public final String detailedContent;

    @JsonProperty("content_type_version")
    public final String contentTypeVersion;

    @JsonProperty("sent_at")
    public final LocalDateTime sentAt;

    @JsonProperty("created_at")
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
