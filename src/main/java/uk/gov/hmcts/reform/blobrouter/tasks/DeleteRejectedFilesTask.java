package uk.gov.hmcts.reform.blobrouter.tasks;

import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.slf4j.Logger;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.reform.blobrouter.tasks.processors.RejectedContainerCleaner;

import static org.slf4j.LoggerFactory.getLogger;
import static uk.gov.hmcts.reform.blobrouter.tasks.utils.LoggingHelper.wrapWithJobLog;
import static uk.gov.hmcts.reform.blobrouter.util.TimeZones.EUROPE_LONDON;

@Component
@ConditionalOnProperty(value = "scheduling.task.delete-rejected-files.enabled")
public class DeleteRejectedFilesTask {

    private static final String TASK_NAME = "delete-rejected-files";
    private static final Logger logger = getLogger(DeleteRejectedFilesTask.class);

    private final RejectedContainerCleaner cleaner;

    public DeleteRejectedFilesTask(RejectedContainerCleaner cleaner) {
        this.cleaner = cleaner;
    }

    @Scheduled(cron = "${scheduling.task.delete-rejected-files.cron}", zone = EUROPE_LONDON)
    @SchedulerLock(name = TASK_NAME)
    public void run() {
        wrapWithJobLog(logger, TASK_NAME, cleaner::cleanUp);
    }
}
