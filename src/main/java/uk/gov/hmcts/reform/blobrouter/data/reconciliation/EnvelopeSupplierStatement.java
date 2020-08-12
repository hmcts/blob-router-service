package uk.gov.hmcts.reform.blobrouter.data.reconciliation;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

public class EnvelopeSupplierStatement {

    public final UUID id;
    public final LocalDate date;
    public final String content;
    public final String contentTypeVersion;
    public final LocalDateTime createdAt;

    public EnvelopeSupplierStatement(
        UUID id,
        LocalDate date,
        String content,
        String contentTypeVersion,
        LocalDateTime createdAt
    ) {
        this.id = id;
        this.date = date;
        this.content = content;
        this.contentTypeVersion = contentTypeVersion;
        this.createdAt = createdAt;
    }
}
