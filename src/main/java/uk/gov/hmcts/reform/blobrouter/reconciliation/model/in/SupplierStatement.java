package uk.gov.hmcts.reform.blobrouter.reconciliation.model.in;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public class SupplierStatement {
    public final List<Envelope> envelopes;

    public SupplierStatement(@JsonProperty("envelopes") List<Envelope> envelopes) {
        this.envelopes = envelopes;
    }
}
