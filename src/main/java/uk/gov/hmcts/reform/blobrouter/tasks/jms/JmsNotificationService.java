package uk.gov.hmcts.reform.blobrouter.tasks.jms;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.blobrouter.data.rejectedenvelope.RejectedEnvelope;
import uk.gov.hmcts.reform.blobrouter.data.rejectedenvelope.RejectedEnvelopeRepository;
import uk.gov.hmcts.reform.blobrouter.servicebus.notifications.model.NotificationMsg;
import uk.gov.hmcts.reform.blobrouter.services.EnvelopeService;

import java.util.List;

/**
 * The `JmsNotificationService` class in Java sends notifications for rejected envelopes via JMS if the property
 * `jms.enabled` is set to true.
 */
@Service
@ConditionalOnProperty(name = "jms.enabled", havingValue = "true")
public class JmsNotificationService {

    private static final Logger log = LoggerFactory.getLogger(JmsNotificationService.class);

    private static final String SERVICE_NAME = "blob_router";

    private final JmsNotificationsPublisher jmsNotificationsPublisher;

    private final EnvelopeService envelopeService;

    private final RejectedEnvelopeRepository rejectedEnvelopeRepository;

    public JmsNotificationService(
        JmsNotificationsPublisher jmsNotificationsPublisher,
        RejectedEnvelopeRepository rejectedEnvelopeRepository,
        EnvelopeService envelopeService
    ) {
        this.jmsNotificationsPublisher = jmsNotificationsPublisher;
        this.rejectedEnvelopeRepository = rejectedEnvelopeRepository;
        this.envelopeService = envelopeService;
    }

    public void sendNotifications() {
        List<RejectedEnvelope> envelopes = rejectedEnvelopeRepository.getRejectedEnvelopes();
        envelopes.forEach(
            env -> {
                log.info(
                    "Send message to notifications queue. File name: {} Container: {}", env.fileName, env.container
                );
                jmsNotificationsPublisher.publish(mapToNotificationMessage(env), env.envelopeId.toString());
                envelopeService.markPendingNotificationAsSent(env.envelopeId);
            }
        );
    }

    /**
     * The function `mapToNotificationMessage` creates a `NotificationMsg` object using information from a
     * `RejectedEnvelope` object.
     *
     * @param envelope The `envelope` parameter in the `mapToNotificationMessage` method is of type `RejectedEnvelope`.
     * @return A `NotificationMsg` object is being returned, which is created using the data from the `RejectedEnvelope`
     *      object `envelope`.
     */
    private NotificationMsg mapToNotificationMessage(RejectedEnvelope envelope) {
        return new NotificationMsg(
            envelope.fileName,
            envelope.container,
            null,
            envelope.errorCode,
            envelope.errorDescription,
            SERVICE_NAME
        );
    }
}
