package uk.gov.hmcts.reform.blobrouter.reconciliation.task;

import org.junit.jupiter.api.Test;
import uk.gov.hmcts.reform.blobrouter.reconciliation.service.CftDetailedReportService;

import java.time.LocalDate;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

class DetailedReportTaskTest {

    @Test
    void should_call_detailed_report_service() {
        // given
        var cftDetailedReportService = mock(CftDetailedReportService.class);

        var task = new DetailedReportTask(cftDetailedReportService);

        // when
        task.run();

        // then
        verify(cftDetailedReportService, times(1)).process(LocalDate.now());
    }

}
