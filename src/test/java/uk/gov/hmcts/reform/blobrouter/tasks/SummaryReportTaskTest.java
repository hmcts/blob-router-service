package uk.gov.hmcts.reform.blobrouter.tasks;

import org.junit.jupiter.api.Test;
import uk.gov.hmcts.reform.blobrouter.reconciliation.service.SummaryReportService;

import java.time.LocalDate;

import static java.time.ZoneOffset.UTC;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

class SummaryReportTaskTest {

    @Test
    void should_call_summary_report_service() {
        // given
        var summaryReportService = mock(SummaryReportService.class);
        var task = new SummaryReportTask(summaryReportService);

        // when
        task.run();

        // then
        verify(summaryReportService, times(1)).process(LocalDate.now(UTC));;
    }
}
