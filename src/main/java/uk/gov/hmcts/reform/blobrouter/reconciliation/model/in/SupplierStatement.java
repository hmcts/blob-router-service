package uk.gov.hmcts.reform.blobrouter.reconciliation.model.in;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.annotations.ApiModelProperty;

import java.util.List;
import javax.validation.constraints.NotNull;

public class SupplierStatement {

    @ApiModelProperty(value = "List of envelopes uploaded", required = true)
    @NotNull
    public final List<Envelope> envelopes;

    public SupplierStatement(@JsonProperty(value = "envelopes", required = true) List<Envelope> envelopes) {
        this.envelopes = envelopes;
    }
}
