package uk.gov.hmcts.reform.blobrouter.reconciliation.report;

import com.fasterxml.jackson.annotation.JsonProperty;

public class SummaryReportItem {

    @JsonProperty("zip_file_name")
    private String zipFileName;

    @JsonProperty("container")
    private String container;

    public SummaryReportItem(String zipFileName, String container) {
        this.zipFileName = zipFileName;
        this.container = container;
    }
}
