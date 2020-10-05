package uk.gov.hmcts.reform.blobrouter.reconciliation.model.out;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.annotations.ApiModelProperty;

public class SuccessfulResponse {

    @ApiModelProperty("ID under which Bulk Scanning has registered the report")
    @JsonProperty("id")
    public final String id;

    @ApiModelProperty(value = "Warning related to the supplied statement")
    @JsonProperty("warning")
    public final String warning;

    public SuccessfulResponse(String id, String warning) {
        this.id = id;
        this.warning = warning;
    }

    public SuccessfulResponse(String id) {
        this(id, null);
    }
}
