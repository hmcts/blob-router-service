package uk.gov.hmcts.reform.blobrouter.data.reports;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
import uk.gov.hmcts.reform.blobrouter.model.out.reports.EnvelopeCountSummaryReportItem;

import java.time.LocalDateTime;
import java.util.List;

public class EnvelopeCountSummaryReportListResponse {

    @JsonProperty("total_received")
    public final int totalReceived;

    @JsonProperty("total_rejected")
    public final int totalRejected;

    @JsonProperty("time_stamp")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    public final LocalDateTime timeStamp;

    @JsonProperty("data")
    public final List<EnvelopeCountSummaryReportItem> items;

    public EnvelopeCountSummaryReportListResponse(
        List<EnvelopeCountSummaryReportItem> items
    ) {
        this.items = items;
        int totalReceivedEnvelopes = 0;
        int totalRejectedEnvelopes = 0;

        for (var item : items) {
            totalReceivedEnvelopes += item.received;
            totalRejectedEnvelopes += item.rejected;
        }

        this.totalReceived = totalReceivedEnvelopes;
        this.totalRejected = totalRejectedEnvelopes;
        timeStamp = LocalDateTime.now();
    }
}
