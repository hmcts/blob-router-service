package uk.gov.hmcts.reform.blobrouter.reconciliation.task;

import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.slf4j.Logger;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.reform.blobrouter.reconciliation.service.SummaryReportService;

import java.time.LocalDate;

import static java.time.ZoneOffset.UTC;
import static org.slf4j.LoggerFactory.getLogger;
import static uk.gov.hmcts.reform.blobrouter.util.TimeZones.EUROPE_LONDON;

@Component
@ConditionalOnProperty(value = "scheduling.task.create-reconciliation-summary-report.enabled")
public class SummaryReportTask {

    private static final String TASK_NAME = "create-reconciliation-summary-report";
    private static final Logger logger = getLogger(SummaryReportTask.class);

    private final SummaryReportService summaryReportService;

    public SummaryReportTask(SummaryReportService summaryReportService) {
        this.summaryReportService = summaryReportService;
    }

    @Scheduled(cron = "${scheduling.task.create-reconciliation-summary-report.cron}", zone = EUROPE_LONDON)
    @SchedulerLock(name = TASK_NAME)
    public void run() {
        logger.info("Started {} job", TASK_NAME);

        summaryReportService.process(LocalDate.now(UTC).minusDays(1));

        logger.info("Finished {} job", TASK_NAME);

    }
}
