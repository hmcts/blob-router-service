package uk.gov.hmcts.reform.blobrouter.services;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import uk.gov.hmcts.reform.blobrouter.data.DbHelper;
import uk.gov.hmcts.reform.blobrouter.data.events.EnvelopeEvent;
import uk.gov.hmcts.reform.blobrouter.data.events.EnvelopeEventRepository;
import uk.gov.hmcts.reform.blobrouter.data.events.EventType;
import uk.gov.hmcts.reform.blobrouter.data.rejectedenvelope.RejectedEnvelopeRepository;
import uk.gov.hmcts.reform.blobrouter.servicebus.notifications.NotificationsPublisher;
import uk.gov.hmcts.reform.blobrouter.servicebus.notifications.model.NotificationMsg;

import java.util.List;

import static java.time.Instant.now;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static uk.gov.hmcts.reform.blobrouter.data.events.EventType.DELETED;

@SpringBootTest
@ExtendWith(MockitoExtension.class)
@ActiveProfiles("db-test")
class NotificationServiceTest {

    @Autowired
    private NotificationService notificationService;
    @Autowired
    private EnvelopeService envelopeService;
    @Autowired
    private RejectedEnvelopeRepository rejectedEnvelopeRepository;
    @Autowired
    private EnvelopeEventRepository envelopeEventRepository;
    @Autowired
    private DbHelper dbHelper;

    @Captor
    private ArgumentCaptor<NotificationMsg> notificationMsgCaptor;

    @Mock
    NotificationsPublisher notificationsPublisher;

    @BeforeEach
    void setUp() {
        notificationService = new NotificationService(
            notificationsPublisher,
            rejectedEnvelopeRepository,
            envelopeService
        );
    }

    @AfterEach
    void tearDown() {
        dbHelper.deleteAll();
    }

    @Test
    void should_send_notifications_for_the_rejected_envelopes() {
        // given
        // rejected envelope
        var envelope1 = envelopeService.createNewEnvelope("bulkscan", "blob1.zip", now());
        envelopeService.markAsRejected(envelope1, "duplicate file");

        // envelope that is not rejected
        var envelope2 = envelopeService.createNewEnvelope("bulkscan", "blob2.zip", now());
        envelopeService.markAsDispatched(envelope2);

        // rejected envelope
        var envelope3 = envelopeService.createNewEnvelope("testcontainer", "blob3.zip", now());
        envelopeService.markAsRejected(envelope3, "invalid signature");
        envelopeService.saveEvent(envelope3, DELETED);

        // when
        notificationService.sendNotifications();

        // then
        verify(notificationsPublisher, times(2)).publish(any());
        List<EnvelopeEvent> envelope1Events = envelopeEventRepository.findForEnvelope(envelope1);
        assertThat(envelope1Events).extracting(e -> e.type).contains(EventType.NOTIFICATION_SENT);

        List<EnvelopeEvent> envelope3Events = envelopeEventRepository.findForEnvelope(envelope3);
        assertThat(envelope3Events).extracting(e -> e.type).contains(EventType.NOTIFICATION_SENT);
    }

    @Test
    void should_not_send_notification_when_notification_was_already_sent() {
        // given
        // rejected and notification_sent
        var envelope1 = envelopeService.createNewEnvelope("bulkscan", "blob1.zip", now());
        envelopeService.markAsRejected(envelope1, "duplicate file");
        envelopeService.saveEvent(envelope1, DELETED);
        envelopeService.saveEvent(envelope1, EventType.NOTIFICATION_SENT);

        // envelope that is not rejected
        var envelope2 = envelopeService.createNewEnvelope("bulkscan", "blob2.zip", now());
        envelopeService.markAsDispatched(envelope2);
        envelopeService.saveEvent(envelope1, DELETED);

        // rejected but notification NOT sent
        var envelope3 = envelopeService.createNewEnvelope("bulkscan", "blob3.zip", now());
        envelopeService.markAsRejected(envelope3, "invalid signature");
        envelopeService.saveEvent(envelope3, DELETED);

        // when
        notificationService.sendNotifications();

        // then
        verify(notificationsPublisher).publish(notificationMsgCaptor.capture());
        verify(notificationsPublisher, times(1)).publish(any());

        NotificationMsg msgCaptorValue = notificationMsgCaptor.getValue();
        assertThat(msgCaptorValue.zipFileName).isEqualTo("blob3.zip");
        assertThat(msgCaptorValue.container).isEqualTo("bulkscan");

        List<EnvelopeEvent> envelope3Events = envelopeEventRepository.findForEnvelope(envelope3);
        assertThat(envelope3Events).extracting(e -> e.type).contains(EventType.NOTIFICATION_SENT);
    }

}
