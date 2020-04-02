package uk.gov.hmcts.reform.blobrouter.tasks;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.reform.blobrouter.services.NotificationService;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.internal.verification.VerificationModeFactory.times;

@ExtendWith(MockitoExtension.class)
class SendNotificationsTaskTest {

    @Test
    void should_call_notifications_service() {
        // given
        var notificationService = mock(NotificationService.class);
        var notificationsTask = new SendNotificationsTask(notificationService);

        // when
        notificationsTask.run();

        // then
        verify(notificationService, times(1)).sendNotifications();
    }
}
