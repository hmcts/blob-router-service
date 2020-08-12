package uk.gov.hmcts.reform.blobrouter.data.reconciliation.model;

import java.time.LocalDate;
import java.util.UUID;

public class NewEnvelopeSupplierStatement {

    public final UUID id;
    public final LocalDate date;
    public final String content;
    public final String contentTypeVersion;

    public NewEnvelopeSupplierStatement(
        UUID id,
        LocalDate date,
        String content,
        String contentTypeVersion
    ) {
        this.id = id;
        this.date = date;
        this.content = content;
        this.contentTypeVersion = contentTypeVersion;
    }
}
