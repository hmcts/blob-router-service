package uk.gov.hmcts.reform.blobrouter.util;

import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;

import static java.time.ZoneOffset.UTC;

public final class DateFormatter {

    private static final String DATETIME_PATTERN = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'";
    private static final DateTimeFormatter formatter = DateTimeFormatter.ofPattern(DATETIME_PATTERN);

    public static String getSimpleDateTime(final Instant instant) {
        return formatter.format(ZonedDateTime.ofInstant(instant, ZoneId.from(UTC)));
    }

    private DateFormatter() {
        // utility class constructor
    }
}
