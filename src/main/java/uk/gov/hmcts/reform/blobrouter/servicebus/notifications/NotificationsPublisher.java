package uk.gov.hmcts.reform.blobrouter.servicebus.notifications;

import com.azure.messaging.servicebus.ServiceBusMessage;
import com.azure.messaging.servicebus.ServiceBusSenderClient;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.blobrouter.servicebus.notifications.model.NotificationMsg;

import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

@Service
public class NotificationsPublisher {

    private static final Logger logger = LoggerFactory.getLogger(NotificationsPublisher.class);

    private final ServiceBusSenderClient queueClient;
    private final ObjectMapper objectMapper;

    public NotificationsPublisher(
        ServiceBusSenderClient queueClient,
        ObjectMapper objectMapper
    ) {
        this.queueClient = queueClient;
        this.objectMapper = objectMapper;
    }

    public void publish(NotificationMsg notificationMsg, String messageId) {
        try {
            String messageBody = objectMapper.writeValueAsString(notificationMsg);

            ServiceBusMessage message = new ServiceBusMessage(messageBody);
            message.setMessageId(messageId);
            message.setContentType(APPLICATION_JSON_VALUE);

            queueClient.sendMessage(message);

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
