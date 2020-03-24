package uk.gov.hmcts.reform.blobrouter.data.notifications;

public class NotificationEnvelope {

    public final String container;
    public final String fileName;
    public final String errorType;
    public final String notes;

    public NotificationEnvelope(
        String container,
        String fileName,
        String errorType,
        String notes
    ) {
        this.container = container;
        this.fileName = fileName;
        this.errorType = errorType;
        this.notes = notes;
    }

}
