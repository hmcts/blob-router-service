package uk.gov.hmcts.reform.blobrouter.util;

import java.time.ZoneId;

/**
 * The TimeZones class provides constants for the Europe/London time zone and its corresponding ZoneId.
 */
public final class TimeZones {

    public static final String EUROPE_LONDON = "Europe/London";
    public static final ZoneId EUROPE_LONDON_ZONE_ID = ZoneId.of(EUROPE_LONDON);

    private TimeZones() {
        // utility class construct
    }
}
