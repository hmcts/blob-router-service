package uk.gov.hmcts.reform.blobrouter.reconciliation.model.in;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

import java.util.List;

public class SupplierStatement {

    @Schema(description = "List of envelopes uploaded", required = true)
    @NotNull
    public final List<Envelope> envelopes;

    public SupplierStatement(@JsonProperty("envelopes") List<Envelope> envelopes) {
        this.envelopes = envelopes;
    }
}
