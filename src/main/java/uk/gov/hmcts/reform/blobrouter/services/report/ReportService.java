package uk.gov.hmcts.reform.blobrouter.services.report;

import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.blobrouter.data.ReportRepository;
import uk.gov.hmcts.reform.blobrouter.model.out.EnvelopeSummaryResponse;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

import static java.time.ZoneOffset.UTC;
import static java.util.stream.Collectors.toList;

@Service
public class ReportService {
    private static final String TIME_FORMAT = "HH:mm:ss";
    private final ReportRepository reportRepository;

    public ReportService(ReportRepository reportRepository) {
        this.reportRepository = reportRepository;
    }

    public List<EnvelopeSummaryResponse> getDailyReport(LocalDate date) {

        var from = date.atStartOfDay().toInstant(UTC);
        var to = date.atStartOfDay().plusDays(1).toInstant(UTC);

        return reportRepository.getEnvelopeSummary(from, to).stream()
                .map(s -> new EnvelopeSummaryResponse(
                        s.container,
                        s.fileName,
                        LocalDateTime.ofInstant(s.fileCreatedAt, UTC).toLocalDate(),
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
            return LocalDateTime.ofInstant(instant, UTC).toLocalDate();
        }
        return null;
    }

    private LocalTime toLocalTime(Instant instant) {
        if (instant != null) {
            return LocalTime.parse(DateTimeFormatter.ofPattern(TIME_FORMAT).format(instant.atZone(UTC)));
        }
        return null;
    }
}
