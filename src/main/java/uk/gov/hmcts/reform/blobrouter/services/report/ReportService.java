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
import java.util.Objects;

import static java.util.stream.Collectors.toList;
import static uk.gov.hmcts.reform.blobrouter.util.TimeZones.EUROPE_LONDON_ZONE_ID;

@Service
public class ReportService {
    private final ReportRepository reportRepository;
    public static final String TEST_CONTAINER = "bulkscan";

    private static final Logger log = LoggerFactory.getLogger(ReportService.class);
    private final ServiceConfiguration serviceConfiguration;

    public ReportService(ReportRepository reportRepository, ServiceConfiguration serviceConfiguration) {
        this.reportRepository = reportRepository;
        this.serviceConfiguration = serviceConfiguration;
    }

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

    public List<EnvelopeCountSummaryReportItem> getCountFor(LocalDate date, boolean includeTestContainer) {
        List<String> containersList = serviceConfiguration.getSourceContainers();
        long start = System.currentTimeMillis();
        final List<EnvelopeCountSummaryReportItem> reportResult = reportRepository.getReportFor(
            date, containersList).stream()
            .filter(it -> includeTestContainer || !Objects.equals(it.container, TEST_CONTAINER))
            .collect(toList());
        log.info("Count summary report took {} ms", System.currentTimeMillis() - start);
        return reportResult;
    }

    private LocalDate toLocalDate(Instant instant) {
        if (instant != null) {
            return LocalDateTime.ofInstant(instant, EUROPE_LONDON_ZONE_ID).toLocalDate();
        }
        return null;
    }

    private LocalTime toLocalTime(Instant instant) {
        if (instant != null) {
            return LocalTime.ofInstant(instant, EUROPE_LONDON_ZONE_ID);
        }
        return null;
    }
}
