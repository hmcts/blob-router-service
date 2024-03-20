package uk.gov.hmcts.reform.blobrouter.services.report;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.blobrouter.config.ServiceConfiguration;
import uk.gov.hmcts.reform.blobrouter.data.reports.ReportRepository;
import uk.gov.hmcts.reform.blobrouter.model.out.EnvelopeSummaryItem;
import uk.gov.hmcts.reform.blobrouter.model.out.reports.EnvelopeCountSummaryReportItem;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

import static java.util.stream.Collectors.toList;
import static uk.gov.hmcts.reform.blobrouter.util.TimeZones.EUROPE_LONDON_ZONE_ID;

/**
 * The `ReportService` class in Java provides methods to retrieve daily
 * envelope summary reports and count summary reports based on specified dates.
 */
@Service
public class ReportService {
    private static final Logger log = LoggerFactory.getLogger(ReportService.class);

    private final ReportRepository reportRepository;
    private final ServiceConfiguration serviceConfiguration;

    public ReportService(ReportRepository reportRepository, ServiceConfiguration serviceConfiguration) {
        this.reportRepository = reportRepository;
        this.serviceConfiguration = serviceConfiguration;
    }

    /**
     * This Java function retrieves a daily report of envelope summary items based on a given date.
     *
     * @param date The `date` parameter in the `getDailyReport` method represents
     *             the specific date for which you want to generate the daily report. This method
     *             retrieves envelope summary items for the given date by querying the `reportRepository`
     *             for data within the time range from the start of the given date to the start of the end date.
     * @return A List of EnvelopeSummaryItem objects representing the daily report for the specified LocalDate date.
     */
    public List<EnvelopeSummaryItem> getDailyReport(LocalDate date) {

        var from = date.atStartOfDay().atZone(EUROPE_LONDON_ZONE_ID).toInstant();
        var to = date.atStartOfDay().plusDays(1).atZone(EUROPE_LONDON_ZONE_ID).toInstant();

        return reportRepository
            .getEnvelopeSummary(from, to)
            .stream()
            .map(s -> new EnvelopeSummaryItem(
                s.container,
                s.fileName,
                toLocalDate(s.fileCreatedAt),
                toLocalTime(s.fileCreatedAt),
                toLocalDate(s.dispatchedAt),
                toLocalTime(s.dispatchedAt),
                s.status.name(),
                s.isDeleted
            ))
            .collect(toList());
    }

    /**
     * This function retrieves envelope count summary report items for a specific
     * date using a list of containers and logs the time taken for the operation.
     *
     * @param date The `date` parameter is of type `LocalDate` and represents the date for which the
     *             count summary report is being generated.
     * @return The method `getCountFor(LocalDate date)` returns a list of `EnvelopeCountSummaryReportItem` objects.
     */
    public List<EnvelopeCountSummaryReportItem> getCountFor(LocalDate date) {
        List<String> containersList = serviceConfiguration.getSourceContainers();
        long start = System.currentTimeMillis();
        final List<EnvelopeCountSummaryReportItem> reportResult = reportRepository.getReportFor(date, containersList);
        log.info("Count summary report took {} ms", System.currentTimeMillis() - start);
        return reportResult;
    }

    /**
     * The function `toLocalDate` converts an `Instant` to a `LocalDate` using the Europe/London time zone.
     *
     * @param instant An Instant object representing a specific moment in time.
     * @return The method `toLocalDate` returns a `LocalDate` object.
     */
    private LocalDate toLocalDate(Instant instant) {
        if (instant != null) {
            return LocalDateTime.ofInstant(instant, EUROPE_LONDON_ZONE_ID).toLocalDate();
        }
        return null;
    }

    /**
     * The function `toLocalTime` converts an `Instant` to a `LocalTime` in the Europe/London time zone.
     *
     * @param instant An Instant object that represents a point in time in UTC.
     * @return The method `toLocalTime` returns a `LocalTime` object representing the time at the specified `Instant` in
     *      the Europe/London time zone. If the input `Instant` is `null`, the method returns `null`.
     */
    private LocalTime toLocalTime(Instant instant) {
        if (instant != null) {
            return LocalTime.ofInstant(instant, EUROPE_LONDON_ZONE_ID);
        }
        return null;
    }
}
