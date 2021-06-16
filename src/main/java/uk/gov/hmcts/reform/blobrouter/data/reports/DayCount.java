package uk.gov.hmcts.reform.blobrouter.data.reports;

import java.time.LocalDate;

public class DayCount {

    public final LocalDate date;
    public final int count;

    public DayCount(LocalDate date, int count) {
        this.date = date;
        this.count = count;
    }
}
