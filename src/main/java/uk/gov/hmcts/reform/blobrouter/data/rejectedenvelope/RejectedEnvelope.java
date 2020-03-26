package uk.gov.hmcts.reform.blobrouter.data.rejectedenvelope;

public class RejectedEnvelope {

    public final String container;
    public final String fileName;
    public final String errorDescription;

    public RejectedEnvelope(
        String container,
        String fileName,
        String errorDescription
    ) {
        this.container = container;
        this.fileName = fileName;
        this.errorDescription = errorDescription;
    }

}
