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
import uk.gov.hmcts.reform.blobrouter.data.envelopes.Envelope;
import uk.gov.hmcts.reform.blobrouter.data.events.EnvelopeEvent;
import uk.gov.hmcts.reform.blobrouter.data.events.EnvelopeEventRepository;
import uk.gov.hmcts.reform.blobrouter.data.events.ErrorCode;
import uk.gov.hmcts.reform.blobrouter.data.events.EventType;
import uk.gov.hmcts.reform.blobrouter.data.rejectedenvelope.RejectedEnvelopeRepository;
import uk.gov.hmcts.reform.blobrouter.servicebus.notifications.NotificationsPublisher;
import uk.gov.hmcts.reform.blobrouter.servicebus.notifications.model.NotificationMsg;

import java.util.List;
import java.util.Optional;

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

    @Captor
    private ArgumentCaptor<String> messageIdCaptor;

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
        var envelopeId1 = envelopeService.createNewEnvelope("bulkscan", "blob1.zip", now());
        envelopeService.markAsRejected(envelopeId1, ErrorCode.ERR_METAFILE_INVALID, "duplicate file");

        // envelope that is not rejected
        var envelopeId2 = envelopeService.createNewEnvelope("bulkscan", "blob2.zip", now());
        envelopeService.markAsDispatched(envelopeId2);

        // rejected envelope
        var envelopeId3 = envelopeService.createNewEnvelope("testcontainer", "blob3.zip", now());
        envelopeService.markAsRejected(envelopeId3, ErrorCode.ERR_SIG_VERIFY_FAILED, "invalid signature");
        envelopeService.saveEvent(envelopeId3, DELETED);

        // when
        notificationService.sendNotifications();

        // then
        verify(notificationsPublisher, times(2)).publish(any(), any());

        Optional<Envelope> envelope1 = envelopeService.findEnvelope(envelopeId1);
        assertThat(envelope1).hasValueSatisfying(env -> assertThat(env.pendingNotification).isFalse());
        List<EnvelopeEvent> envelope1Events = envelopeEventRepository.findForEnvelope(envelopeId1);
        assertThat(envelope1Events).extracting(e -> e.type).contains(EventType.NOTIFICATION_SENT);

        Optional<Envelope> envelope3 = envelopeService.findEnvelope(envelopeId3);
        assertThat(envelope3).hasValueSatisfying(env -> assertThat(env.pendingNotification).isFalse());
        List<EnvelopeEvent> envelope3Events = envelopeEventRepository.findForEnvelope(envelopeId3);
        assertThat(envelope3Events).extracting(e -> e.type).contains(EventType.NOTIFICATION_SENT);
    }

    @Test
    void should_not_send_notification_when_notification_was_already_sent() {
        // given
        // rejected and notification_sent
        var envelopeId1 = envelopeService.createNewEnvelope("bulkscan", "blob1.zip", now());
        envelopeService.markAsRejected(envelopeId1, ErrorCode.ERR_METAFILE_INVALID, "duplicate file");
        envelopeService.saveEvent(envelopeId1, DELETED);
        envelopeService.markPendingNotificationAsSent(envelopeId1);

        // envelope that is not rejected
        var envelopeId2 = envelopeService.createNewEnvelope("bulkscan", "blob2.zip", now());
        envelopeService.markAsDispatched(envelopeId2);
        envelopeService.saveEvent(envelopeId1, DELETED);

        // rejected but notification NOT sent
        var envelopeId3 = envelopeService.createNewEnvelope("bulkscan", "blob3.zip", now());
        envelopeService.markAsRejected(envelopeId3, ErrorCode.ERR_SIG_VERIFY_FAILED, "invalid signature");
        envelopeService.saveEvent(envelopeId3, DELETED);

        // rejected but notification NOT sent
        var envelopeId4 = envelopeService.createNewEnvelope("bulkscan", "blob4.zip", now());
        envelopeService.markAsRejected(envelopeId4, ErrorCode.ERR_SIG_VERIFY_FAILED, "invalid signature");
        envelopeService.saveEvent(envelopeId4, DELETED);

        // rejected but notification NOT sent
        var envelopeId5 = envelopeService.createNewEnvelope("bulkscan", "blob5.zip", now());
        envelopeService.markAsRejected(envelopeId5, ErrorCode.ERR_SIG_VERIFY_FAILED, "invalid signature");
        envelopeService.saveEvent(envelopeId5, DELETED);

        // rejected but notification NOT sent
        var envelopeId6 = envelopeService.createNewEnvelope("bulkscan", "blob6.zip", now());
        envelopeService.markAsRejected(envelopeId6, ErrorCode.ERR_SIG_VERIFY_FAILED, "invalid signature");
        envelopeService.saveEvent(envelopeId6, DELETED);

        // rejected but notification NOT sent
        var envelopeId7 = envelopeService.createNewEnvelope("bulkscan", "blob7.zip", now());
        envelopeService.markAsRejected(envelopeId7, ErrorCode.ERR_SIG_VERIFY_FAILED, "invalid signature");
        envelopeService.saveEvent(envelopeId7, DELETED);

        // rejected but notification NOT sent
        var envelopeId8 = envelopeService.createNewEnvelope("bulkscan", "blob8.zip", now());
        envelopeService.markAsRejected(envelopeId8, ErrorCode.ERR_SIG_VERIFY_FAILED, "invalid signature");
        envelopeService.saveEvent(envelopeId8, DELETED);

        // rejected but notification NOT sent
        var envelopeId9 = envelopeService.createNewEnvelope("bulkscan", "blob9.zip", now());
        envelopeService.markAsRejected(envelopeId9, ErrorCode.ERR_SIG_VERIFY_FAILED, "invalid signature");
        envelopeService.saveEvent(envelopeId9, DELETED);

        // rejected but notification NOT sent
        var envelopeId10 = envelopeService.createNewEnvelope("bulkscan", "blob10.zip", now());
        envelopeService.markAsRejected(envelopeId10, ErrorCode.ERR_SIG_VERIFY_FAILED, "invalid signature");
        envelopeService.saveEvent(envelopeId10, DELETED);

        // rejected but notification NOT sent
        var envelopeId11 = envelopeService.createNewEnvelope("bulkscan", "blob11.zip", now());
        envelopeService.markAsRejected(envelopeId11, ErrorCode.ERR_SIG_VERIFY_FAILED, "invalid signature");
        envelopeService.saveEvent(envelopeId11, DELETED);

        // rejected but notification NOT sent
        var envelopeId12 = envelopeService.createNewEnvelope("bulkscan", "blob12.zip", now());
        envelopeService.markAsRejected(envelopeId12, ErrorCode.ERR_SIG_VERIFY_FAILED, "invalid signature");
        envelopeService.saveEvent(envelopeId12, DELETED);

        // rejected but notification NOT sent
        var envelopeId13 = envelopeService.createNewEnvelope("bulkscan", "blob13.zip", now());
        envelopeService.markAsRejected(envelopeId13, ErrorCode.ERR_SIG_VERIFY_FAILED, "invalid signature");
        envelopeService.saveEvent(envelopeId13, DELETED);


        // when
        notificationService.sendNotifications(); //1st time
        try {
            new Thread().sleep(2000L);
        } catch (Exception ex) {

        }
        notificationService.sendNotifications(); //2nd time

        verify(notificationsPublisher, times(11)).publish(notificationMsgCaptor.capture(), messageIdCaptor.capture());

        NotificationMsg msgCaptorValue = notificationMsgCaptor.getValue();
        String messageId = messageIdCaptor.getValue();
        assertThat(msgCaptorValue.zipFileName).isEqualTo("blob13.zip");
        assertThat(msgCaptorValue.container).isEqualTo("bulkscan");
        assertThat(messageId).isEqualTo(envelopeId3.toString());

        Optional<Envelope> envelope3 = envelopeService.findEnvelope(envelopeId3);
        assertThat(envelope3).hasValueSatisfying(env -> assertThat(env.pendingNotification).isFalse());
        List<EnvelopeEvent> envelope3Events = envelopeEventRepository.findForEnvelope(envelopeId3);
        assertThat(envelope3Events).extracting(e -> e.type).contains(EventType.NOTIFICATION_SENT);
    }

}
