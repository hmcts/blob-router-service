package uk.gov.hmcts.reform.blobrouter.reconciliation.model.out;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.annotations.ApiModelProperty;

public class SuccessfulResponse {

    @ApiModelProperty(
        name = "ID",
        notes = "ID under which Bulk Scanning has registered the report"
    )
    @JsonProperty("id")
    public final String id;

    public SuccessfulResponse(String id) {
        this.id = id;
    }
}
