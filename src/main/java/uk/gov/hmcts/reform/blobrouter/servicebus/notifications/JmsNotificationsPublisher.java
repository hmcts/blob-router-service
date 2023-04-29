package uk.gov.hmcts.reform.blobrouter.servicebus.notifications;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.jms.core.MessagePostProcessor;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.blobrouter.servicebus.notifications.model.NotificationMsg;

import javax.jms.JMSException;
import javax.jms.Message;

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

    public void publish(NotificationMsg notificationMsg, String messageId) {
        try {
            String messageBody = objectMapper.writeValueAsString(notificationMsg);
            jmsTemplate.convertAndSend("notifications", messageBody, new MessagePostProcessor() {
                @Override
                public Message postProcessMessage(Message message) throws JMSException {
                    message.setJMSMessageID(messageId);
                    return message;
                }
            });

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
