package uk.gov.hmcts.reform.blobrouter.data.model;

import java.time.Instant;

public class EventRecord {

    public final long id;
    public final String container;
    public final String fileName;
    public final Instant createdAt;
    public final Event event;
    public final String notes;

    public EventRecord(
        long id,
        String container,
        String fileName,
        Instant createdAt,
        Event event,
        String notes
    ) {
        this.id = id;
        this.container = container;
        this.fileName = fileName;
        this.createdAt = createdAt;
        this.event = event;
        this.notes = notes;
    }
}
