package uk.gov.hmcts.reform.blobrouter.reconciliation.model.in;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public class Report {
    public final List<Envelope> envelopes;

    public Report(@JsonProperty("envelopes") List<Envelope> envelopes) {
        this.envelopes = envelopes;
    }
}
