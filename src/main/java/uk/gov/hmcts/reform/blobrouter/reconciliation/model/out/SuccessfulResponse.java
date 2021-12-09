package uk.gov.hmcts.reform.blobrouter.reconciliation.model.out;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;

public class SuccessfulResponse {

    @Schema(description = "ID under which Bulk Scanning has registered the report")
    @JsonProperty("id")
    public final String id;

    @Schema(description = "Warning related to the supplied statement")
    @JsonProperty("warning")
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public final String warning;

    public SuccessfulResponse(String id, String warning) {
        this.id = id;
        this.warning = warning;
    }

    public SuccessfulResponse(String id) {
        this(id, null);
    }
}
