package uk.gov.hmcts.reform.blobrouter.data.envelopes;

import java.time.Instant;
import java.util.UUID;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Table;

@Entity
@Table(name = "envelopes")
public class Envelope {

    @Id
    @GeneratedValue
    private UUID id;

    private String container;
    private String fileName;
    private Instant createdAt;
    private Instant fileCreatedAt;
    private Instant dispatchedAt;
    private Status status;
    private boolean isDeleted;
    private boolean pendingNotification;

    public Envelope() {

    }

    public boolean getPendingNotification() {
        return pendingNotification;
    }

    public Instant getFileCreatedAt() {
        return fileCreatedAt;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public UUID getId() {
        return id;
    }

    public Instant getDispatchedAt() {
        return dispatchedAt;
    }

    public String getContainer() {
        return container;
    }

    public String getFileName() {
        return fileName;
    }

    public Status getStatus() {
        return status;
    }

    public boolean getIsDeleted() {
        return isDeleted;
    }

    public Envelope(
        UUID id,
        String container,
        String fileName,
        Instant createdAt,
        Instant fileCreatedAt,
        Instant dispatchedAt,
        Status status,
        boolean isDeleted,
        boolean pendingNotification
    ) {
        this.id = id;
        this.container = container;
        this.fileName = fileName;
        this.createdAt = createdAt;
        this.fileCreatedAt = fileCreatedAt;
        this.dispatchedAt = dispatchedAt;
        this.status = status;
        this.isDeleted = isDeleted;
        this.pendingNotification = pendingNotification;
    }



    public String getBasicInfo() {
        return String.format(
            "Envelope ID: %s, File name: %s, Container: %s, Status: %s",
            this.id,
            this.fileName,
            this.container,
            this.status
        );
    }
}
