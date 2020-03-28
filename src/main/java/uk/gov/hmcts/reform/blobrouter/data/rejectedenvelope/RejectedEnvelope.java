package uk.gov.hmcts.reform.blobrouter.data.rejectedenvelope;

import java.util.UUID;

public class RejectedEnvelope {

    public final UUID envelopeId;
    public final String container;
    public final String fileName;
    public final String errorDescription;

    public RejectedEnvelope(
        UUID envelopeId,
        String container,
        String fileName,
        String errorDescription
    ) {
        this.envelopeId = envelopeId;
        this.container = container;
        this.fileName = fileName;
        this.errorDescription = errorDescription;
    }

}
