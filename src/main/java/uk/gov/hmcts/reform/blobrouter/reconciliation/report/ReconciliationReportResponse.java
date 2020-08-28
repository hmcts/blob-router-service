package uk.gov.hmcts.reform.blobrouter.reconciliation.report;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public class ReconciliationReportResponse {


    public final List<DiscrepancyItem> items;

    @JsonCreator
    public ReconciliationReportResponse(@JsonProperty("discrepancies") List<DiscrepancyItem> items) {
        this.items = items;
    }
}
