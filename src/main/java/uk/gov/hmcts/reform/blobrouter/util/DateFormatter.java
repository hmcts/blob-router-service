package uk.gov.hmcts.reform.blobrouter.util;

import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;

import static java.time.ZoneOffset.UTC;

/**
 * The `DateFormatter` class provides a method to format an `Instant` object into a
 * string representing the date and time in UTC.
 */
public final class DateFormatter {

    private static final String DATETIME_PATTERN = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'";
    private static final DateTimeFormatter formatter = DateTimeFormatter.ofPattern(DATETIME_PATTERN);

    /**
     * The function `getSimpleDateTime` takes an `Instant` and returns a formatted `String`
     * representing the date and time in UTC.
     *
     * @param instant An Instant object representing a point in time.
     * @return The method `getSimpleDateTime` returns a formatted string representing the date
     * and time of the provided `Instant` object in UTC time zone.
     */
    public static String getSimpleDateTime(final Instant instant) {
        return formatter.format(ZonedDateTime.ofInstant(instant, ZoneId.from(UTC)));
    }

    private DateFormatter() {
        // utility class constructor
    }
}
