package uk.gov.hmcts.reform.blobrouter.data.model;

import java.time.Instant;

public class EnvelopeSummary {

    private final String container;
    private final String fileName;
    private final Instant fileCreatedAt;
    private final Instant dispatchedAt;
    private final Status status;
    private final Event lastEvent;
    private final String eventNotes;

    public EnvelopeSummary(
        String container,
        String fileName,
        Instant fileCreatedAt,
        Instant dispatchedAt,
        Status status,
        Event lastEvent,
        String eventNotes
    ) {
        this.container = container;
        this.fileName = fileName;
        this.fileCreatedAt = fileCreatedAt;
        this.dispatchedAt = dispatchedAt;
        this.status = status;
        this.lastEvent = lastEvent;
        this.eventNotes = eventNotes;
    }
}
