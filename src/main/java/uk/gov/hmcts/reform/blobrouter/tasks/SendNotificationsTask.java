package uk.gov.hmcts.reform.blobrouter.tasks;

import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.slf4j.Logger;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.reform.blobrouter.services.NotificationService;

import static org.slf4j.LoggerFactory.getLogger;
import static uk.gov.hmcts.reform.blobrouter.util.TimeZones.EUROPE_LONDON;

/**
 * This Java class represents a scheduled task for sending notifications, conditionally enabled based on properties and
 * expressions.
 */
@Component
@ConditionalOnProperty(value = "scheduling.task.send-notifications.enabled")
@ConditionalOnExpression("!${jms.enabled}")
public class SendNotificationsTask {

    private static final String TASK_NAME = "send-notifications";
    private static final Logger logger = getLogger(SendNotificationsTask.class);

    private final NotificationService notificationService;

    public SendNotificationsTask(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    @Scheduled(cron = "${scheduling.task.send-notifications.cron}", zone = EUROPE_LONDON)
    @SchedulerLock(name = TASK_NAME)
    public void run() {
        logger.info("Started {} job", TASK_NAME);

        notificationService.sendNotifications();

        logger.info("Finished {} job", TASK_NAME);
    }
}
