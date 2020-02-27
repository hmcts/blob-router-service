package uk.gov.hmcts.reform.blobrouter.services.report;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.reform.blobrouter.data.ReportRepository;
import uk.gov.hmcts.reform.blobrouter.data.model.EnvelopeSummary;
import uk.gov.hmcts.reform.blobrouter.model.out.EnvelopeSummaryResponse;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static uk.gov.hmcts.reform.blobrouter.data.model.Status.DISPATCHED;
import static uk.gov.hmcts.reform.blobrouter.data.model.Status.REJECTED;
import static uk.gov.hmcts.reform.blobrouter.util.DateTimeUtils.instant;
import static uk.gov.hmcts.reform.blobrouter.util.DateTimeUtils.localDate;
import static uk.gov.hmcts.reform.blobrouter.util.DateTimeUtils.localTime;

@ExtendWith(MockitoExtension.class)
class ReportServiceTest {
    public static final String CONTAINER_1 = "cont1";
    public static final String FILE_NAME_1 = "file1.zip";
    public static final String FILE_NAME_2 = "file2.zip";
    private ReportService reportService;

    @Mock
    private ReportRepository reportRepository;

    @BeforeEach
    void setUp() {
        reportService = new ReportService(reportRepository);
    }

    @Test
    void getDailyReport_should_convert_date_into_date_range() {
        // given
        LocalDate dt = LocalDate.of(2019, 1, 14);
        Instant from = instant("2019-01-14 00:00:00");
        Instant to = instant("2019-01-15 00:00:00");

        // when
        reportService.getDailyReport(dt);

        // then
        verify(reportRepository).getEnvelopeSummary(from, to);
        verifyNoMoreInteractions(reportRepository);
    }

    @Test
    void getDailyReport_should_convert_repo_result() {
        // given
        Instant inst1 = instant("2019-01-14 10:11:12");
        Instant inst2 = instant("2019-01-15 11:12:13");
        Instant inst3 = instant("2019-01-16 12:13:14");
        EnvelopeSummary es1 = new EnvelopeSummary(CONTAINER_1, FILE_NAME_1, inst1, inst2, DISPATCHED, true);
        EnvelopeSummary es2 = new EnvelopeSummary(CONTAINER_1, FILE_NAME_2, inst3, null, REJECTED, false);
        given(reportRepository.getEnvelopeSummary(any(Instant.class), any(Instant.class)))
            .willReturn(asList(es1, es2));

        LocalDate dt = LocalDate.of(2019, 1, 14);

        // when
        List<EnvelopeSummaryResponse> res = reportService.getDailyReport(dt);

        // then
        assertThat(res)
            .extracting(env -> tuple(
                env.container,
                env.fileName,
                env.dateReceived,
                env.timeReceived,
                env.dateProcessed,
                env.timeProcessed,
                env.status,
                env.isDeleted
            ))
            .containsExactlyInAnyOrder(
                tuple(
                    CONTAINER_1,
                    FILE_NAME_1,
                    localDate("2019-01-14"),
                    localTime("10:11:12"),
                    localDate("2019-01-15"),
                    localTime("11:12:13"),
                    DISPATCHED.name(),
                    true
                ),
                tuple(
                    CONTAINER_1,
                    FILE_NAME_2,
                    localDate("2019-01-16"),
                    localTime("12:13:14"),
                    null,
                    null,
                    REJECTED.name(),
                    false
                )
            );
    }
}
