package uk.gov.hmcts.reform.blobrouter.reconciliation.report;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Objects;

public class SummaryReportItem {

    @JsonProperty("zip_file_name")
    public final String zipFileName;

    @JsonProperty("container")
    public final String container;

    public SummaryReportItem(String zipFileName, String container) {
        this.zipFileName = zipFileName;
        this.container = container;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        SummaryReportItem that = (SummaryReportItem) o;
        return Objects.equals(zipFileName, that.zipFileName)
            && Objects.equals(container, that.container);
    }

    @Override
    public int hashCode() {
        return Objects.hash(zipFileName, container);
    }
}
