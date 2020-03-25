package uk.gov.hmcts.reform.blobrouter.servicebus.notifications;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.microsoft.azure.servicebus.IMessage;
import com.microsoft.azure.servicebus.Message;
import com.microsoft.azure.servicebus.QueueClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.blobrouter.servicebus.notifications.model.NotificationMsg;

import java.util.UUID;

import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

@Service
public class NotificationsPublisher {

    private static final Logger logger = LoggerFactory.getLogger(NotificationsPublisher.class);

    private final QueueClient queueClient;
    private final ObjectMapper objectMapper;

    public NotificationsPublisher(
        QueueClient queueClient,
        ObjectMapper objectMapper
    ) {
        this.queueClient = queueClient;
        this.objectMapper = objectMapper;
    }

    public void publish(NotificationMsg notificationMsg) {
        try {
            String messageBody = objectMapper.writeValueAsString(notificationMsg);

            IMessage message = new Message(
                UUID.randomUUID().toString(),
                messageBody,
                APPLICATION_JSON_VALUE
            );

            queueClient.send(message);

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
                        + "File name: %s, Container: %s",
                    notificationMsg.zipFileName,
                    notificationMsg.container
                ),
                ex
            );
        }
    }
}
