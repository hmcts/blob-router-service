package uk.gov.hmcts.reform.blobrouter.model.out;

import java.time.LocalDate;
import java.util.List;

public class DailyReportResponse {
    public final LocalDate date;
    public final List<EnvelopeSummaryResponse> envelopes;
    public final int dispatched;
    public final int rejected;

    public DailyReportResponse(
        LocalDate date,
        List<EnvelopeSummaryResponse> envelopes,
        int dispatched,
        int rejected
    ) {
        this.date = date;
        this.envelopes = envelopes;
        this.dispatched = dispatched;
        this.rejected = rejected;
    }
}
