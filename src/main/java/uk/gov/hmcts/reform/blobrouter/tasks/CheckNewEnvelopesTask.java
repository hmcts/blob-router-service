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
@ConditionalOnProperty(value = "scheduling.task.check-new-envelopes.enabled")
public class CheckNewEnvelopesTask {

    private static final String TASK_NAME = "check-new-envelopes";
    private static final Logger logger = getLogger(CheckNewEnvelopesTask.class);

    private final NewEnvelopesFinder newEnvelopesFinder;

    public CheckNewEnvelopesTask(NewEnvelopesFinder newEnvelopesFinder) {
        this.newEnvelopesFinder = newEnvelopesFinder;
    }

    @Scheduled(cron = "${scheduling.task.check-new-envelopes.cron}", zone = EUROPE_LONDON)
    @SchedulerLock(name = TASK_NAME)
    public void run() {
        logger.info("Started {} job", TASK_NAME);

        newEnvelopesFinder.checkNewCftEnvelopesCreated();

        // TODO: enable once 'crime' envelopes are enabled. (and update test)
        // newEnvelopesFinder.checkNewEnvelopesCreatedInContainer("crime", "Crime");

        newEnvelopesFinder.checkNewEnvelopesCreatedInContainer("pcq", "PCQ");

        logger.info("Finished {} job", TASK_NAME);
    }
}
