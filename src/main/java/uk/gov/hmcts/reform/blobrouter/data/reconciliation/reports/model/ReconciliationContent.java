package uk.gov.hmcts.reform.blobrouter.data.reconciliation.reports.model;

import java.util.UUID;

public class ReconciliationContent {

    public final UUID id;
    public final String content;
    public final String contentVersion;

    public ReconciliationContent(UUID id, String content, String contentVersion) {
        this.id = id;
        this.content = content;
        this.contentVersion = contentVersion;
    }
}
