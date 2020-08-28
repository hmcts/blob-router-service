package uk.gov.hmcts.reform.blobrouter.reconciliation.report;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class DiscrepancyItem {

    public final String zipFileName;

    public final String container;

    public final String type;

    public final String stated;

    public final String actual;

    @JsonCreator
    public DiscrepancyItem(
        @JsonProperty("zip_file_name") String zipFileName,
        @JsonProperty("container") String container,
        @JsonProperty("type") String type,
        @JsonProperty("stated") String stated,
        @JsonProperty("actual") String actual
    ) {
        this.zipFileName = zipFileName;
        this.container = container;
        this.type = type;
        this.stated = stated;
        this.actual = actual;
    }
}
