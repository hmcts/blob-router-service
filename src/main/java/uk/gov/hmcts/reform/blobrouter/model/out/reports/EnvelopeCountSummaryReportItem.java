package uk.gov.hmcts.reform.blobrouter.model.out.reports;

import java.time.LocalDate;

public class EnvelopeCountSummaryReportItem {

    public final int received;
    public final int rejected;
    public final String container;
    public final LocalDate date;

    // region constructor
    public EnvelopeCountSummaryReportItem(int received, int rejected, String container, LocalDate date) {
        this.received = received;
        this.rejected = rejected;
        this.container = container;
        this.date = date;
    }
    // endregion
}
