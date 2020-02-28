package uk.gov.hmcts.reform.blobrouter.services.report;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.blobrouter.data.ReportRepository;
import uk.gov.hmcts.reform.blobrouter.model.out.EnvelopeSummaryItem;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

import static java.util.stream.Collectors.toList;
import static uk.gov.hmcts.reform.blobrouter.util.TimeZones.EUROPE_LONDON_ZONE_ID;

@Service
public class ReportService {
    private final ReportRepository reportRepository;

    public ReportService(ReportRepository reportRepository) {
        this.reportRepository = reportRepository;
    }

    public List<EnvelopeSummaryItem> getDailyReport(LocalDate date) {

        var from = date.atStartOfDay().atZone(EUROPE_LONDON_ZONE_ID).toInstant();
        var to = date.atStartOfDay().plusDays(1).atZone(EUROPE_LONDON_ZONE_ID).toInstant();

        return reportRepository.getEnvelopeSummary(from, to).stream()
                .map(s -> new EnvelopeSummaryItem(
                        s.container,
                        s.fileName,
                        toLocalDate(s.fileCreatedAt),
                        toLocalTime(s.fileCreatedAt),
                        toLocalDate(s.dispatchedAt),
                        toLocalTime(s.dispatchedAt),
                        s.status.name(),
                        s.isDeleted
                    )
                )
                .collect(toList());
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
