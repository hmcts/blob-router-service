package uk.gov.hmcts.reform.blobrouter.tasks;

import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.slf4j.Logger;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.reform.blobrouter.config.ServiceConfiguration;
import uk.gov.hmcts.reform.blobrouter.services.storage.DuplicateFileHandler;

import static org.slf4j.LoggerFactory.getLogger;
import static uk.gov.hmcts.reform.blobrouter.util.TimeZones.EUROPE_LONDON;

@Component
@ConditionalOnProperty(value = "scheduling.task.handle-rejected-files.enabled")
@EnableConfigurationProperties(ServiceConfiguration.class)
public class RejectDuplicatesTask {

    private static final String TASK_NAME = "reject-duplicates";
    private static final Logger logger = getLogger(HandleRejectedFilesTask.class);

    private final DuplicateFileHandler duplicateFileHandler;

    public RejectDuplicatesTask(DuplicateFileHandler duplicateFileHandler) {
        this.duplicateFileHandler = duplicateFileHandler;
    }

    @Scheduled(cron = "${scheduling.task.reject-duplicates.cron}", zone = EUROPE_LONDON)
    @SchedulerLock(name = TASK_NAME)
    public void run() {
        logger.info("Started {} job", TASK_NAME);
        duplicateFileHandler.handle();
        logger.info("Finished {} job", TASK_NAME);
    }
}
