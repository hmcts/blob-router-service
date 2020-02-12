package uk.gov.hmcts.reform.blobrouter.features;

import com.azure.core.http.rest.PagedFlux;
import com.azure.core.http.rest.PagedIterable;
import com.azure.storage.blob.BlobServiceClient;
import com.microsoft.applicationinsights.TelemetryClient;
import com.microsoft.applicationinsights.telemetry.RequestTelemetry;
import net.javacrumbs.shedlock.core.LockConfiguration;
import net.javacrumbs.shedlock.core.LockProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import reactor.core.publisher.Mono;
import uk.gov.hmcts.reform.blobrouter.tasks.DeleteDispatchedFilesTask;
import uk.gov.hmcts.reform.blobrouter.tasks.DeleteRejectedFilesTask;
import uk.gov.hmcts.reform.blobrouter.tasks.HandleRejectedFilesTask;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;
import static org.mockito.BDDMockito.given;
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
        "scheduling.task.send-daily-report.cron: */1 * * * * *"
    }
)
@ActiveProfiles("integration-test")
public class SchedulerConfigTest {

    @Autowired
    private BlobServiceClient storageClient;

    @SpyBean
    private LockProvider lockProvider;

    @SpyBean
    private TelemetryClient telemetryClient;

    @BeforeEach
    void stubStorageClientCalls() {
        // used in DeleteRejectedFiles task
        // other tasks firstly checks DB before calling storage
        given(storageClient.listBlobContainers())
            .willReturn(new PagedIterable<>(new PagedFlux<>(Mono::empty)));
    }

    @Test
    public void should_integrate_with_shedlock() throws Exception {
        var configCaptor = ArgumentCaptor.forClass(LockConfiguration.class);

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
                "send-daily-report"
            );

        // and
        var telemetryCaptor = ArgumentCaptor.forClass(RequestTelemetry.class);

        verify(telemetryClient, atLeastOnce()).trackRequest(telemetryCaptor.capture());
        assertThat(telemetryCaptor.getAllValues())
            .extracting(telemetry -> tuple(telemetry.getName(), telemetry.isSuccess()))
            .contains(
                tuple("Schedule /" + DeleteDispatchedFilesTask.class.getSimpleName(), true),
                tuple("Schedule /" + DeleteRejectedFilesTask.class.getSimpleName(), true),
                tuple("Schedule /" + HandleRejectedFilesTask.class.getSimpleName(), true)
            );
    }
}
