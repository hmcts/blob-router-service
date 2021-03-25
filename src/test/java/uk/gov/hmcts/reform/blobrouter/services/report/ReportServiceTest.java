package uk.gov.hmcts.reform.blobrouter.services.report;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.reform.blobrouter.config.ServiceConfiguration;
import uk.gov.hmcts.reform.blobrouter.data.reports.EnvelopeSummary;
import uk.gov.hmcts.reform.blobrouter.data.reports.ReportRepository;
import uk.gov.hmcts.reform.blobrouter.model.out.EnvelopeSummaryItem;
import uk.gov.hmcts.reform.blobrouter.model.out.reports.EnvelopeCountSummaryReportItem;

import java.time.Instant;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;

import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static uk.gov.hmcts.reform.blobrouter.data.envelopes.Status.DISPATCHED;
import static uk.gov.hmcts.reform.blobrouter.data.envelopes.Status.REJECTED;
import static uk.gov.hmcts.reform.blobrouter.util.DateTimeUtils.instant;
import static uk.gov.hmcts.reform.blobrouter.util.DateTimeUtils.localDate;
import static uk.gov.hmcts.reform.blobrouter.util.DateTimeUtils.localTime;

@ExtendWith(MockitoExtension.class)
class ReportServiceTest {
    private static final String TEST_CONTAINER = "bulkscan";
    private static final String CRIME_CONTAINER = "crime";
    private static final String PCQ_CONTAINER = "pcq";
    private static final String PROBATE_CONTAINER = "probate";

    private ReportService reportService;

    @Mock
    private ReportRepository reportRepository;

    @Mock ServiceConfiguration serviceConfiguration;

    @BeforeEach
    void setUp() {
        reportService = new ReportService(reportRepository, serviceConfiguration);
    }

    @Test
    void getDailyReport_should_convert_date_into_date_range() {
        // given
        LocalDate dt = LocalDate.of(2019, 1, 14);

        // when
        reportService.getDailyReport(dt);

        // then
        Instant expectedFrom = instant("2019-01-14 00:00:00");
        Instant expectedTo = instant("2019-01-15 00:00:00");

        verify(reportRepository).getEnvelopeSummary(expectedFrom, expectedTo);
        verifyNoMoreInteractions(reportRepository);
    }

    @Test
    void getDailyReport_should_convert_repo_result() {
        // given
        final String container1 = "cont1";
        final String fileName1 = "file1.zip";
        final String fileName2 = "file2.zip";

        EnvelopeSummary es1 = new EnvelopeSummary(
            container1,
            fileName1,
            instant("2019-01-14 10:11:12"),
            instant("2019-01-15 11:12:13"),
            DISPATCHED,
            true
        );
        EnvelopeSummary es2 = new EnvelopeSummary(
            container1,
            fileName2,
            instant("2019-01-16 12:13:14"),
            null,
            REJECTED,
            false
        );
        given(reportRepository.getEnvelopeSummary(any(Instant.class), any(Instant.class)))
            .willReturn(asList(es1, es2));

        LocalDate dt = LocalDate.of(2019, 1, 14);

        // when
        List<EnvelopeSummaryItem> res = reportService.getDailyReport(dt);

        // then
        assertThat(res)
            .usingFieldByFieldElementComparator()
            .containsExactlyInAnyOrder(
                new EnvelopeSummaryItem(
                    container1,
                    fileName1,
                    localDate("2019-01-14"),
                    localTime("10:11:12"),
                    localDate("2019-01-15"),
                    localTime("11:12:13"),
                    DISPATCHED.name(),
                    true
                ),
                new EnvelopeSummaryItem(
                    container1,
                    fileName2,
                    localDate("2019-01-16"),
                    localTime("12:13:14"),
                    null,
                    null,
                    REJECTED.name(),
                    false
                )
            );
    }

    @Test
    void should_return_envelope_count_summary_list_for_matching_date() {
        //given
        LocalDate dateCountedFor = LocalDate.of(2020, 5, 22);
        List<String> containerNames = Arrays.asList("crime", "pcq", "probate");

        given(serviceConfiguration.getSourceContainers())
            .willReturn(containerNames);

        given(reportRepository.getReportFor(dateCountedFor, containerNames))
            .willReturn(asList(
                new EnvelopeCountSummaryReportItem(5, 2, CRIME_CONTAINER, dateCountedFor),
                new EnvelopeCountSummaryReportItem(54, 12, PCQ_CONTAINER, dateCountedFor),
                new EnvelopeCountSummaryReportItem(25, 8, PROBATE_CONTAINER, dateCountedFor)
            ));
        boolean includeTestContainer = false;

        //when
        List<EnvelopeCountSummaryReportItem> result = reportService.getCountFor(
            dateCountedFor,
            includeTestContainer
        );

        //then
        assertThat(result)
            .usingFieldByFieldElementComparator()
            .containsExactly(
                new EnvelopeCountSummaryReportItem(5, 2, CRIME_CONTAINER, dateCountedFor),
                new EnvelopeCountSummaryReportItem(54, 12, PCQ_CONTAINER, dateCountedFor),
                new EnvelopeCountSummaryReportItem(25, 8, PROBATE_CONTAINER, dateCountedFor)
            );
    }

    @Test
    void should_return_envelope_count_summary_list_for_matching_date_with_test_container() {
        //given
        LocalDate dateCountedFor = LocalDate.of(2019, 8, 14);
        boolean includeTestContainer = true;
        List<String> containerNames = Arrays.asList("crime", "pcq", "probate", TEST_CONTAINER);

        given(serviceConfiguration.getSourceContainers())
            .willReturn(containerNames);

        given(reportRepository.getReportFor(dateCountedFor, containerNames))
            .willReturn(asList(
                new EnvelopeCountSummaryReportItem(5, 2, CRIME_CONTAINER, dateCountedFor),
                new EnvelopeCountSummaryReportItem(54, 12, PCQ_CONTAINER, dateCountedFor),
                new EnvelopeCountSummaryReportItem(25, 8, PROBATE_CONTAINER, dateCountedFor),
                new EnvelopeCountSummaryReportItem(15, 6, TEST_CONTAINER, dateCountedFor)
            ));

        //when
        List<EnvelopeCountSummaryReportItem> result = reportService.getCountFor(
            dateCountedFor,
            includeTestContainer
        );

        //then
        assertThat(result)
            .usingFieldByFieldElementComparator()
            .containsExactly(
                new EnvelopeCountSummaryReportItem(5, 2, CRIME_CONTAINER, dateCountedFor),
                new EnvelopeCountSummaryReportItem(54, 12, PCQ_CONTAINER, dateCountedFor),
                new EnvelopeCountSummaryReportItem(25, 8, PROBATE_CONTAINER, dateCountedFor),
                new EnvelopeCountSummaryReportItem(15, 6, TEST_CONTAINER, dateCountedFor)
            );
    }

    @Test
    void should_return_envelope_count_summary_list_for_matching_date_with_out_test_container() {
        //given
        LocalDate dateCountedFor = LocalDate.of(2018, 2, 10);
        boolean includeTestContainer = false;
        List<String> containerNames = Arrays.asList("crime", "pcq", "probate", "bulkscan");

        given(serviceConfiguration.getSourceContainers())
            .willReturn(containerNames);

        given(reportRepository.getReportFor(dateCountedFor, containerNames))
            .willReturn(asList(
                new EnvelopeCountSummaryReportItem(23, 5, CRIME_CONTAINER, dateCountedFor),
                new EnvelopeCountSummaryReportItem(105, 28, PCQ_CONTAINER, dateCountedFor),
                new EnvelopeCountSummaryReportItem(125, 18, PROBATE_CONTAINER, dateCountedFor),
                new EnvelopeCountSummaryReportItem(135, 36, TEST_CONTAINER, dateCountedFor)
            ));

        //when
        List<EnvelopeCountSummaryReportItem> result = reportService.getCountFor(
            dateCountedFor,
            includeTestContainer
        );

        //then
        assertThat(result)
            .usingFieldByFieldElementComparator()
            .containsExactly(
                new EnvelopeCountSummaryReportItem(23, 5, CRIME_CONTAINER, dateCountedFor),
                new EnvelopeCountSummaryReportItem(105, 28, PCQ_CONTAINER, dateCountedFor),
                new EnvelopeCountSummaryReportItem(125, 18, PROBATE_CONTAINER, dateCountedFor)
            );
    }

}
