package uk.gov.hmcts.reform.blobrouter.data.rejectedenvelope;

import uk.gov.hmcts.reform.blobrouter.data.events.ErrorCode;

import java.util.UUID;

public class RejectedEnvelope {

    public final UUID envelopeId;
    public final String container;
    public final String fileName;
    public final ErrorCode errorCode;
    public final String errorDescription;

    public RejectedEnvelope(
        UUID envelopeId,
        String container,
        String fileName,
        ErrorCode errorCode,
        String errorDescription
    ) {
        this.envelopeId = envelopeId;
        this.container = container;
        this.fileName = fileName;
        this.errorCode = errorCode;
        this.errorDescription = errorDescription;
    }

}
