package uk.gov.hmcts.reform.blobrouter.reconciliation.report;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public class SummaryReport {

    @JsonProperty("actual_count")
    private int actualCount;

    @JsonProperty("reported_count")
    private int reportedCount;

    @JsonProperty("received_but_not_reported")
    private List<SummaryReportItem> receivedButNotReported;

    @JsonProperty("reported_but_not_received")
    private List<SummaryReportItem> reportedButNotReceived;

    public SummaryReport(
        int actualCount,
        int reportedCount,
        List<SummaryReportItem> receivedButNotReported,
        List<SummaryReportItem> reportedButNotReceived
    ) {
        this.actualCount = actualCount;
        this.reportedCount = reportedCount;
        this.receivedButNotReported = receivedButNotReported;
        this.reportedButNotReceived = reportedButNotReceived;
    }
}
