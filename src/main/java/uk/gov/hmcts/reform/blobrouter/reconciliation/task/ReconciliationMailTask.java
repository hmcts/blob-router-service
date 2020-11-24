package uk.gov.hmcts.reform.blobrouter.reconciliation.task;

import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.slf4j.Logger;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.reform.blobrouter.config.TargetStorageAccount;
import uk.gov.hmcts.reform.blobrouter.reconciliation.service.ReconciliationMailService;

import java.time.LocalDate;
import java.util.List;

import static java.time.ZoneOffset.UTC;
import static org.slf4j.LoggerFactory.getLogger;
import static uk.gov.hmcts.reform.blobrouter.config.TargetStorageAccount.CFT;
import static uk.gov.hmcts.reform.blobrouter.config.TargetStorageAccount.CRIME;
import static uk.gov.hmcts.reform.blobrouter.util.TimeZones.EUROPE_LONDON;

@Component
@ConditionalOnProperty(value = "scheduling.task.send-reconciliation-report-mail.enabled")
public class ReconciliationMailTask {

    private static final String TASK_NAME = "send-reconciliation-report-mail";
    private static final Logger logger = getLogger(ReconciliationMailTask.class);

    private static final List<TargetStorageAccount> AVAILABLE_ACCOUNTS =
        List.of(CFT, CRIME);

    private final ReconciliationMailService reconciliationMailService;

    public ReconciliationMailTask(ReconciliationMailService reconciliationMailService) {
        this.reconciliationMailService = reconciliationMailService;
    }

    @Scheduled(cron = "${scheduling.task.send-reconciliation-report-mail.cron}", zone = EUROPE_LONDON)
    @SchedulerLock(name = TASK_NAME)
    public void run() {
        logger.info("Started {} job", TASK_NAME);

        reconciliationMailService.process(LocalDate.now(UTC).minusDays(1), AVAILABLE_ACCOUNTS);

        logger.info("Finished {} job", TASK_NAME);
    }
}
