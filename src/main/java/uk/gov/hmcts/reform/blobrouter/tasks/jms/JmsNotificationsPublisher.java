package uk.gov.hmcts.reform.blobrouter.tasks.jms;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.blobrouter.servicebus.notifications.NotificationsPublishingException;
import uk.gov.hmcts.reform.blobrouter.servicebus.notifications.model.NotificationMsg;

import javax.jms.JMSException;
import javax.jms.Message;

/**
 * The `JmsNotificationsPublisher` class in Java is a service component that publishes
 * notification messages to a JMS queue if the `jms.enabled` property is set to true, handling message
 * serialization and logging relevant information.
 */
@Service
@ConditionalOnProperty(name = "jms.enabled", havingValue = "true")
public class JmsNotificationsPublisher {

    private static final Logger logger = LoggerFactory.getLogger(JmsNotificationsPublisher.class);

    @Autowired
    private JmsTemplate jmsTemplate;

    private final ObjectMapper objectMapper;

    public JmsNotificationsPublisher(
        JmsTemplate jmsTemplate,
        ObjectMapper objectMapper
    ) {
        this.jmsTemplate = jmsTemplate;
        this.objectMapper = objectMapper;
    }

    /**
     * The `publish` method sends a notification message to a JMS queue with specified message ID and logs relevant
     * information.
     *
     * @param notificationMsg The `notificationMsg` parameter is an object of type `NotificationMsg`
     *                        which contains information about a notification message. It likely includes
     *                        details such as `zipFileName`, `container`, and `errorCode`.
     * @param messageId The `messageId` parameter in the `publish` method is used to set the JMS Message ID
     *                  for the message being sent to the "notifications" queue. This ID helps uniquely
     *                  identify the message within the messaging system.
     */
    public void publish(NotificationMsg notificationMsg, String messageId) {
        try {
            String messageBody = objectMapper.writeValueAsString(notificationMsg);
            jmsTemplate.convertAndSend("notifications", messageBody);

            logger.info(
                "Sent message to Notifications queue. File name: {} Container: {} Error code: {}",
                notificationMsg.zipFileName,
                notificationMsg.container,
                notificationMsg.errorCode
            );
        } catch (Exception ex) {
            throw new NotificationsPublishingException(
                String.format(
                    "An error occurred when trying to publish notification for "
                        + "File name: %s, Container: %s, Message Id: %s",
                    notificationMsg.zipFileName,
                    notificationMsg.container,
                    messageId
                ),
                ex
            );
        }
    }
}
