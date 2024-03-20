package uk.gov.hmcts.reform.blobrouter.tasks;

import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.slf4j.Logger;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.reform.blobrouter.services.NewEnvelopesFinder;

import static org.slf4j.LoggerFactory.getLogger;
import static uk.gov.hmcts.reform.blobrouter.util.TimeZones.EUROPE_LONDON;

/**
 * This Java class represents a scheduled task that checks for new envelopes created in
 * different containers and logs the start and end of the job.
 */
@Component
@ConditionalOnProperty(value = "scheduling.task.check-new-envelopes.enabled")
public class CheckNewEnvelopesTask {

    private static final String TASK_NAME = "check-new-envelopes";
    private static final Logger logger = getLogger(CheckNewEnvelopesTask.class);

    private final NewEnvelopesFinder newEnvelopesFinder;

    public CheckNewEnvelopesTask(NewEnvelopesFinder newEnvelopesFinder) {
        this.newEnvelopesFinder = newEnvelopesFinder;
    }

    /**
     * This Java function is scheduled to run at a specific time, checks for new envelopes created in
     * different containers, and logs the start and end of the job.
     */
    @Scheduled(cron = "${scheduling.task.check-new-envelopes.cron}", zone = EUROPE_LONDON)
    @SchedulerLock(name = TASK_NAME)
    public void run() {
        logger.debug("Started {} job", TASK_NAME);

        newEnvelopesFinder.checkNewCftEnvelopesCreated();

        // TODO: enable once 'crime' envelopes are enabled. (and update test)
        // newEnvelopesFinder.checkNewEnvelopesCreatedInContainer("crime", "Crime");

        // TODO: enable once 'pcq' envelopes are enabled. (and update test)
        // newEnvelopesFinder.checkNewEnvelopesCreatedInContainer("pcq", "PCQ");

        logger.debug("Finished {} job", TASK_NAME);
    }
}
