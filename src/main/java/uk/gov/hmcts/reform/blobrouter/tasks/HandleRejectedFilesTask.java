package uk.gov.hmcts.reform.blobrouter.tasks;

import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.slf4j.Logger;
import org.springframework.scheduling.annotation.Scheduled;
import uk.gov.hmcts.reform.blobrouter.services.storage.RejectedFilesHandler;

import static org.slf4j.LoggerFactory.getLogger;
import static uk.gov.hmcts.reform.blobrouter.util.TimeZones.EUROPE_LONDON;

//@Component
//@ConditionalOnProperty(value = "scheduling.task.handle-rejected-files.enabled")
//@EnableConfigurationProperties(ServiceConfiguration.class)
public class HandleRejectedFilesTask {

    private static final String TASK_NAME = "handle-rejected-files";
    private static final Logger logger = getLogger(HandleRejectedFilesTask.class);

    private final RejectedFilesHandler rejectedFilesHandler;

    public HandleRejectedFilesTask(RejectedFilesHandler rejectedFilesHandler) {
        this.rejectedFilesHandler = rejectedFilesHandler;
    }

    @Scheduled(cron = "${scheduling.task.handle-rejected-files.cron}", zone = EUROPE_LONDON)
    @SchedulerLock(name = TASK_NAME)
    public void run() {
        logger.info("Started {} job", TASK_NAME);

        rejectedFilesHandler.handle();

        logger.info("Finished {} job", TASK_NAME);
    }
}
