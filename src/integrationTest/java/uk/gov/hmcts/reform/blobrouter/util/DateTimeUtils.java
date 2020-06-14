package uk.gov.hmcts.reform.blobrouter.util;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import static uk.gov.hmcts.reform.blobrouter.util.TimeZones.EUROPE_LONDON_ZONE_ID;

public final class DateTimeUtils {

    private DateTimeUtils() {
    }

    static final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    public static Instant instant(String string) {
        return LocalDateTime.parse(string, formatter).atZone(EUROPE_LONDON_ZONE_ID).toInstant();
    }

    public static Instant toLocalTimeZone(Instant instant) {
        if (instant != null) {
            return instant.atZone(EUROPE_LONDON_ZONE_ID).toInstant();
        }
        return null;
    }
}
