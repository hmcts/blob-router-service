package uk.gov.hmcts.reform.blobrouter.tasks;

import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.slf4j.Logger;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.reform.blobrouter.services.JmsNotificationService;

import static org.slf4j.LoggerFactory.getLogger;
import static uk.gov.hmcts.reform.blobrouter.util.TimeZones.EUROPE_LONDON;

@Component
@ConditionalOnProperty(value = "scheduling.task.send-notifications.enabled")
@ConditionalOnExpression("${jms.enabled}")
public class JmsSendNotificationsTask {

    private static final String TASK_NAME = "send-notifications";
    private static final Logger logger = getLogger(JmsSendNotificationsTask.class);

    private final JmsNotificationService jmsNotificationService;

    public JmsSendNotificationsTask(JmsNotificationService jmsNotificationService) {
        this.jmsNotificationService = jmsNotificationService;
    }

    @Scheduled(cron = "${scheduling.task.send-notifications.cron}", zone = EUROPE_LONDON)
    @SchedulerLock(name = TASK_NAME)
    public void run() {
        logger.info("Started {} job", TASK_NAME);

        jmsNotificationService.sendNotifications();

        logger.info("Finished {} job", TASK_NAME);
    }
}
