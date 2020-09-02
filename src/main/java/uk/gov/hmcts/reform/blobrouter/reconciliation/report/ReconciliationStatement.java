package uk.gov.hmcts.reform.blobrouter.reconciliation.report;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.LocalDate;
import java.util.List;

public class ReconciliationStatement {

    @JsonProperty("date")
    public final LocalDate date;

    @JsonProperty("envelopes")
    public final List<ReportedZipFile> envelopes;

    public ReconciliationStatement(
        LocalDate date,
        List<ReportedZipFile> envelopes
    ) {
        this.date = date;
        this.envelopes = envelopes;
    }
}
