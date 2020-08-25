package uk.gov.hmcts.reform.blobrouter.data.reports;

import java.util.UUID;

public class ReconciliationReportContent {

    public final UUID id;
    public final String content;
    public final String contentVersion;

    public ReconciliationReportContent(UUID id, String content, String contentVersion) {
        this.id = id;
        this.content = content;
        this.contentVersion = contentVersion;
    }
}
