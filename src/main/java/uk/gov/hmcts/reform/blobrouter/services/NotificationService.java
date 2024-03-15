package uk.gov.hmcts.reform.blobrouter.services;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.blobrouter.data.rejectedenvelope.RejectedEnvelope;
import uk.gov.hmcts.reform.blobrouter.data.rejectedenvelope.RejectedEnvelopeRepository;
import uk.gov.hmcts.reform.blobrouter.servicebus.notifications.NotificationsPublisher;
import uk.gov.hmcts.reform.blobrouter.servicebus.notifications.model.NotificationMsg;

import java.util.List;

/**
 * The `NotificationService` class in Java handles sending notifications for rejected envelopes,
 * mapping envelope data to notification messages.
 */
@Service
@ConditionalOnExpression("!${jms.enabled}")
public class NotificationService {

    private static final Logger log = LoggerFactory.getLogger(NotificationService.class);

    private static final String SERVICE_NAME = "blob_router";

    private final NotificationsPublisher notificationsPublisher;

    private final EnvelopeService envelopeService;

    private final RejectedEnvelopeRepository rejectedEnvelopeRepository;

    public NotificationService(
        NotificationsPublisher notificationsPublisher,
        RejectedEnvelopeRepository rejectedEnvelopeRepository,
        EnvelopeService envelopeService
    ) {
        this.notificationsPublisher = notificationsPublisher;
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
                notificationsPublisher.publish(mapToNotificationMessage(env), env.envelopeId.toString());
                envelopeService.markPendingNotificationAsSent(env.envelopeId);
            }
        );
    }

    /**
     * The function `mapToNotificationMessage` creates a `NotificationMsg` object using information from a
     * `RejectedEnvelope` object.
     *
     * @param envelope The `mapToNotificationMessage` method takes a `RejectedEnvelope` object as a parameter.
     *                 The `RejectedEnvelope` object likely contains information about a rejected message
     *                 or file, such as the file name, container, error code, and error description.
     * @return A `NotificationMsg` object is being returned, which is created using the data from the
     *      `RejectedEnvelope` object `envelope`.
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
