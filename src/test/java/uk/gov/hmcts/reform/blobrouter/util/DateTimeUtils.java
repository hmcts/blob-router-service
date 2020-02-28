package uk.gov.hmcts.reform.blobrouter.util;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

import static uk.gov.hmcts.reform.blobrouter.util.TimeZones.EUROPE_LONDON_ZONE_ID;

public final class DateTimeUtils {

    private DateTimeUtils() {
    }

    private static final DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("HH:mm:ss");

    public static Instant instant(String string) {
        return LocalDateTime.parse(string, dateTimeFormatter).atZone(EUROPE_LONDON_ZONE_ID).toInstant();
    }

    public static LocalDate localDate(String string) {
        return LocalDate.parse(string, dateFormatter);
    }

    public static LocalTime localTime(String string) {
        return LocalTime.parse(string, timeFormatter);
    }
}
