package uk.gov.hmcts.reform.blobrouter.data.reports;

import java.time.LocalDate;

public interface EnvelopeCountSummaryItem {
    LocalDate getDate();

    String getContainer();

    int getReceived();

    int getRejected();
}
