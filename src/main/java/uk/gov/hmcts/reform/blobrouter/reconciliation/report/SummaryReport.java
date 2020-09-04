package uk.gov.hmcts.reform.blobrouter.reconciliation.report;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public class SummaryReport {

    @JsonProperty("actual_count")
    private int actualCount;

    @JsonProperty("received_but_not_reported")
    private List<SummaryReportItem> receivedButNotReported;

    @JsonProperty("reported_count")
    private int reportedCount;

    @JsonProperty("reported_but_not_received")
    private List<SummaryReportItem> reportedButNotReceived;

    public SummaryReport(
        int actualCount,
        List<SummaryReportItem> receivedButNotReported,
        int reportedCount,
        List<SummaryReportItem> reportedButNotReceived
    ) {
        this.actualCount = actualCount;
        this.receivedButNotReported = receivedButNotReported;
        this.reportedCount = reportedCount;
        this.reportedButNotReceived = reportedButNotReceived;
    }
}
