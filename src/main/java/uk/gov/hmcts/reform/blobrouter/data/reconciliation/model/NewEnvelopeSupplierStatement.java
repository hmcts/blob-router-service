package uk.gov.hmcts.reform.blobrouter.data.reconciliation.model;

import java.time.LocalDate;

public class NewEnvelopeSupplierStatement {

    public final LocalDate date;
    public final String content;
    public final String contentTypeVersion;

    public NewEnvelopeSupplierStatement(
        LocalDate date,
        String content,
        String contentTypeVersion
    ) {
        this.date = date;
        this.content = content;
        this.contentTypeVersion = contentTypeVersion;
    }
}
