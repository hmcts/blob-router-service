package uk.gov.hmcts.reform.blobrouter.reconciliation.task;

import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.slf4j.Logger;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.reform.blobrouter.reconciliation.service.CftDetailedReportService;

import java.time.LocalDate;

import static org.slf4j.LoggerFactory.getLogger;
import static uk.gov.hmcts.reform.blobrouter.util.TimeZones.EUROPE_LONDON;

/**
 * The DetailedReportTask class in Java runs a scheduled task to create a
 * reconciliation detailed report using a specified cron expression and time zone.
 */
@Component
@ConditionalOnProperty(value = "scheduling.task.create-reconciliation-detailed-report.enabled")
public class DetailedReportTask {

    private static final String TASK_NAME = "create-reconciliation-detailed-report";
    private static final Logger logger = getLogger(DetailedReportTask.class);

    private final CftDetailedReportService cftDetailedReportService;

    public DetailedReportTask(CftDetailedReportService cftDetailedReportService) {
        this.cftDetailedReportService = cftDetailedReportService;
    }

    /**
     * This Java function runs a scheduled task to create a reconciliation detailed report using a specified cron
     * expression and time zone.
     */
    @Scheduled(cron = "${scheduling.task.create-reconciliation-detailed-report.cron}", zone = EUROPE_LONDON)
    @SchedulerLock(name = TASK_NAME)
    public void run() {
        logger.info("Started {} job", TASK_NAME);

        cftDetailedReportService.process(LocalDate.now().minusDays(1));

        logger.info("Finished {} job", TASK_NAME);
    }
}
