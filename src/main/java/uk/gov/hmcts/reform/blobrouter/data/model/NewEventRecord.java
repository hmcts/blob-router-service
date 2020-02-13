package uk.gov.hmcts.reform.blobrouter.data.model;

public class NewEventRecord {

    public final String container;
    public final String fileName;
    public final Event event;
    public final String notes;

    public NewEventRecord(
        String container,
        String fileName,
        Event event,
        String notes
    ) {
        this.container = container;
        this.fileName = fileName;
        this.event = event;
        this.notes = notes;
    }

    public NewEventRecord(
        String container,
        String fileName,
        Event event
    ) {
        this(container, fileName, event, null);
    }
}
