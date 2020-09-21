package uk.gov.hmcts.reform.blobrouter.reconciliation.task;

import org.junit.jupiter.api.Test;
import uk.gov.hmcts.reform.blobrouter.config.TargetStorageAccount;
import uk.gov.hmcts.reform.blobrouter.reconciliation.service.ReconciliationMailService;

import java.time.LocalDate;
import java.util.List;

import static java.time.ZoneOffset.UTC;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static uk.gov.hmcts.reform.blobrouter.config.TargetStorageAccount.CFT;
import static uk.gov.hmcts.reform.blobrouter.config.TargetStorageAccount.CRIME;

class ReconciliationMailTaskTest {

    @Test
    void should_call_summary_report_service() {
        // given
        var reconciliationMailService = mock(ReconciliationMailService.class);
        var task = new ReconciliationMailTask(reconciliationMailService);

        // when
        task.run();

        // then
        verify(reconciliationMailService, times(1))
            .process(LocalDate.now(UTC), List.of(CFT, CRIME));;
    }
}
