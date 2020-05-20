package uk.gov.hmcts.reform.blobrouter.services;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.gov.hmcts.reform.blobrouter.data.rejectedenvelope.RejectedEnvelope;
import uk.gov.hmcts.reform.blobrouter.data.rejectedenvelope.RejectedEnvelopeRepository;
import uk.gov.hmcts.reform.blobrouter.servicebus.notifications.NotificationsPublisher;
import uk.gov.hmcts.reform.blobrouter.servicebus.notifications.model.NotificationMsg;

import java.util.List;

//@Service
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
                notificationsPublisher.publish(mapToNotificationMessage(env));
                envelopeService.markPendingNotificationAsSent(env.envelopeId);
            }
        );
    }

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
