package uk.gov.hmcts.reform.blobrouter.services;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.reform.blobrouter.data.rejectedenvelope.RejectedEnvelope;
import uk.gov.hmcts.reform.blobrouter.data.rejectedenvelope.RejectedEnvelopeRepository;
import uk.gov.hmcts.reform.blobrouter.servicebus.notifications.NotificationsPublisher;
import uk.gov.hmcts.reform.blobrouter.servicebus.notifications.model.NotificationMsg;

import java.util.UUID;

import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

@ExtendWith(MockitoExtension.class)
class NotificationServiceTest {

    @Mock
    private NotificationsPublisher notificationsPublisher;

    @Mock
    private RejectedEnvelopeRepository rejectedEnvelopeRepository;

    @Mock
    private EnvelopeService envelopeService;

    private NotificationService service;

    @BeforeEach
    void setUp() {
        service = new NotificationService(notificationsPublisher, rejectedEnvelopeRepository, envelopeService);
    }

    @Test
    void should_publish_message_for_all_rejected_envelopes() {
        // given
        UUID envelopeId1 = UUID.randomUUID();
        UUID envelopeId2 = UUID.randomUUID();
        given(rejectedEnvelopeRepository.getRejectedEnvelopes()).willReturn(
            asList(
                new RejectedEnvelope(envelopeId1, "c1", "test1.zip", "notes1"),
                new RejectedEnvelope(envelopeId2, "c2", "test2.zip", "notes1")
            )
        );

        // when
        service.sendNotifications();

        // then
        verify(rejectedEnvelopeRepository).getRejectedEnvelopes();
        verify(notificationsPublisher, times(2)).publish(any(NotificationMsg.class));
        verify(envelopeService).markPendingNotificationAsSent(envelopeId1);
        verify(envelopeService).markPendingNotificationAsSent(envelopeId2);
    }

    @Test
    void should_not_call_publish_when_no_rejected_envelopes_exist() {
        // given
        given(rejectedEnvelopeRepository.getRejectedEnvelopes()).willReturn(emptyList());

        // when
        service.sendNotifications();

        // then
        verify(rejectedEnvelopeRepository).getRejectedEnvelopes();
        verifyNoInteractions(notificationsPublisher, envelopeService);
    }
}
