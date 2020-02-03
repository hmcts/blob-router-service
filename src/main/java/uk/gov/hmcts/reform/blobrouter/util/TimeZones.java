package uk.gov.hmcts.reform.blobrouter.util;

import java.time.ZoneId;
import java.time.ZonedDateTime;

import static java.time.LocalDateTime.now;

public final class TimeZones {

    public static final String EUROPE_LONDON = "Europe/London";
    public static final ZoneId EUROPE_LONDON_ZONE_ID = ZoneId.of(EUROPE_LONDON);

    private TimeZones() {
        // utility class construct
    }

    public static ZonedDateTime nowInLondon() {
        return now().atZone(ZoneId.of(EUROPE_LONDON));
    }
}
