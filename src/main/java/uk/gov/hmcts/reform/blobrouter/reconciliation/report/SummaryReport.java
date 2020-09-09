package uk.gov.hmcts.reform.blobrouter.reconciliation.report;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public class SummaryReport {

    @JsonProperty("actual_count")
    public final int actualCount;

    @JsonProperty("reported_count")
    public final int reportedCount;

    @JsonProperty("received_but_not_reported")
    public final List<SummaryReportItem> receivedButNotReported;

    @JsonProperty("reported_but_not_received")
    public final List<SummaryReportItem> reportedButNotReceived;

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
