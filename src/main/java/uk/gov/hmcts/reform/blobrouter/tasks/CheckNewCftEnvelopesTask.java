package uk.gov.hmcts.reform.blobrouter.tasks;

import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.slf4j.Logger;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.reform.blobrouter.services.NewEnvelopesFinder;

import static org.slf4j.LoggerFactory.getLogger;
import static uk.gov.hmcts.reform.blobrouter.util.TimeZones.EUROPE_LONDON;

@Component
@ConditionalOnProperty(value = "scheduling.task.check-new-envelopes.cft.enabled")
public class CheckNewCftEnvelopesTask {

    private static final String TASK_NAME = "check-new-cft-envelopes";
    private static final Logger logger = getLogger(CheckNewCftEnvelopesTask.class);

    private final NewEnvelopesFinder newEnvelopesFinder;

    public CheckNewCftEnvelopesTask(NewEnvelopesFinder newEnvelopesFinder) {
        this.newEnvelopesFinder = newEnvelopesFinder;
    }

    @Scheduled(cron = "${scheduling.task.check-new-envelopes.cron}", zone = EUROPE_LONDON)
    @SchedulerLock(name = TASK_NAME)
    public void run() {
        logger.info("Started {} job", TASK_NAME);

        newEnvelopesFinder.checkNewCftEnvelopesCreated();

        logger.info("Finished {} job", TASK_NAME);
    }
}
