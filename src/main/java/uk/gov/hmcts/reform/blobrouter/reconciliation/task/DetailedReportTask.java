package uk.gov.hmcts.reform.blobrouter.reconciliation.task;

import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.slf4j.Logger;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.reform.blobrouter.config.TargetStorageAccount;
import uk.gov.hmcts.reform.blobrouter.reconciliation.service.DetailedReportService;

import java.time.LocalDate;

import static org.slf4j.LoggerFactory.getLogger;
import static uk.gov.hmcts.reform.blobrouter.util.TimeZones.EUROPE_LONDON;

@Component
@ConditionalOnProperty(value = "scheduling.task.create-reconciliation-detailed-report.enabled")
public class DetailedReportTask {

    private static final String TASK_NAME = "create-reconciliation-detailed-report";
    private static final Logger logger = getLogger(DetailedReportTask.class);

    private final DetailedReportService detailedReportService;

    public DetailedReportTask(DetailedReportService detailedReportService) {
        this.detailedReportService = detailedReportService;
    }

    @Scheduled(cron = "${scheduling.task.create-reconciliation-detailed-report.cron}", zone = EUROPE_LONDON)
    @SchedulerLock(name = TASK_NAME)
    public void run() {
        logger.info("Started {} job", TASK_NAME);

        try {
            detailedReportService.process(LocalDate.now(), TargetStorageAccount.BULKSCAN);
        } catch (Exception ex) {
            logger.error("Error occurred in {} task", TASK_NAME);
        }
        logger.info("Finished {} job", TASK_NAME);
    }
}
