package uk.gov.hmcts.reform.blobrouter.util;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import static java.time.ZoneOffset.UTC;

public final class DateTimeUtils {

    private DateTimeUtils() {
    }

    static final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    public static Instant instant(String string) {
        return LocalDateTime.parse(string, formatter).toInstant(UTC);
    }
}
