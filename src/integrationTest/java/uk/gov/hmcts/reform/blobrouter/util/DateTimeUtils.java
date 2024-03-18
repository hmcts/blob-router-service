package uk.gov.hmcts.reform.blobrouter.util;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import static uk.gov.hmcts.reform.blobrouter.util.TimeZones.EUROPE_LONDON_ZONE_ID;

public final class DateTimeUtils {

    private DateTimeUtils() {
    }

    static final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    /**
     * The function `instant` parses a string into a LocalDateTime, converts it to an Instant in the Europe/London time
     * zone, and returns the Instant.
     *
     * @param string The `string` parameter is a date and time string that you want to convert to an `Instant` object.
     * @return An `Instant` object is being returned.
     */
    public static Instant instant(String string) {
        return LocalDateTime.parse(string, formatter).atZone(EUROPE_LONDON_ZONE_ID).toInstant();
    }
}
