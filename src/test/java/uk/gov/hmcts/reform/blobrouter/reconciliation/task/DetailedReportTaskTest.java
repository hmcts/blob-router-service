package uk.gov.hmcts.reform.blobrouter.reconciliation.task;

import org.junit.jupiter.api.Test;
import uk.gov.hmcts.reform.blobrouter.reconciliation.service.DetailedReportService;

import java.time.LocalDate;

import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static uk.gov.hmcts.reform.blobrouter.config.TargetStorageAccount.BULKSCAN;

class DetailedReportTaskTest {

    @Test
    void should_call_summary_report_service() {
        // given
        var detailedReportService = mock(DetailedReportService.class);

        var task = new DetailedReportTask(detailedReportService);

        // when
        task.run();

        // then
        verify(detailedReportService, times(1)).process(LocalDate.now(), BULKSCAN);
    }

    @Test
    void should_not_throw_exception_when_detailedReportService_throws_exception() {
        // given
        var detailedReportService = mock(DetailedReportService.class);

        LocalDate date = LocalDate.now();

        willThrow(new RuntimeException("Process Error")).given(detailedReportService).process(date, BULKSCAN);

        var task = new DetailedReportTask(detailedReportService);

        // when
        task.run();

        // then
        verify(detailedReportService, times(1)).process(LocalDate.now(), BULKSCAN);

    }
}
