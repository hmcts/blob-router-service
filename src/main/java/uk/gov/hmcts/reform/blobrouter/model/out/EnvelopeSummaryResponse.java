package uk.gov.hmcts.reform.blobrouter.model.out;

import java.time.LocalDate;
import java.time.LocalTime;

public class EnvelopeSummaryResponse {
    public final String container;
    public final String fileName;
    public final LocalDate dateReceived;
    public final LocalTime timeReceived;
    public final LocalDate dateProcessed;
    public final LocalTime timeProcessed;
    public final String status;
    public final boolean isDeleted;

    public EnvelopeSummaryResponse(
        String container,
        String fileName,
        LocalDate dateReceived,
        LocalTime timeReceived,
        LocalDate dateProcessed,
        LocalTime timeProcessed,
        String status,
        boolean isDeleted
    ) {
        this.container = container;
        this.fileName = fileName;
        this.dateReceived = dateReceived;
        this.timeReceived = timeReceived;
        this.dateProcessed = dateProcessed;
        this.timeProcessed = timeProcessed;
        this.status = status;
        this.isDeleted = isDeleted;
    }
}
