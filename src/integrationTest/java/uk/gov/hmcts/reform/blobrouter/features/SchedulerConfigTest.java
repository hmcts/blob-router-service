package uk.gov.hmcts.reform.blobrouter.features;

import net.javacrumbs.shedlock.core.LockConfiguration;
import net.javacrumbs.shedlock.core.LockProvider;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Profile;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.verify;

@SpringBootTest
@TestPropertySource(
    properties = {
        "scheduling.task.delete-dispatched-files.enabled=true",
        "scheduling.task.delete-dispatched-files.cron: */1 * * * * *",
        "scheduling.task.delete-rejected-files.enabled=true",
        "scheduling.task.delete-rejected-files.cron: */1 * * * * *",
        "scheduling.task.reject-duplicates.enabled=true",
        "scheduling.task.reject-duplicates.cron: */1 * * * * *",
        "scheduling.task.handle-rejected-files.enabled=true",
        "scheduling.task.handle-rejected-files.cron: */1 * * * * *",
        "scheduling.task.check-new-envelopes.cron: */1 * * * * *",
        "scheduling.task.check-new-envelopes.enabled=true",
        "scheduling.task.handle-rejected-files.cron: */1 * * * * *",
        "reports.recipients=test@test",
        "scheduling.task.send-daily-report.enabled=true",
        "scheduling.task.send-daily-report.cron: */1 * * * * *",
        "scheduling.task.send-notifications.enabled=true",
        "scheduling.task.send-notifications.cron: */1 * * * * *",
        "scheduling.task.send-reconciliation-report-mail.enabled=true",
        "scheduling.task.send-reconciliation-report-mail.cron: */1 * * * * *",
        "reconciliation.report.mail-from=sender@test",
        "reconciliation.report.mail-recipients=recipient@test",
        "reconciliation.report.skip-empty=false"
    }
)
@Profile("integration-test")
public class SchedulerConfigTest {

    @MockitoSpyBean
    private LockProvider lockProvider;

    @Test
    public void should_integrate_with_shedlock() throws Exception {
        ArgumentCaptor<LockConfiguration> configCaptor = ArgumentCaptor.forClass(LockConfiguration.class);

        // wait for asynchronous run of the scheduled task in background
        Thread.sleep(2000);

        verify(lockProvider, atLeastOnce()).lock(configCaptor.capture());
        assertThat(configCaptor.getAllValues())
            .extracting(lc -> lc.getName())
            .containsOnly(
                "delete-dispatched-files",
                "delete-rejected-files",
                "reject-duplicates",
                "handle-rejected-files",
                "check-new-envelopes",
                "handle-rejected-files",
                "send-daily-report",
                "send-notifications",
                "send-reconciliation-report-mail"
            );
    }
}
