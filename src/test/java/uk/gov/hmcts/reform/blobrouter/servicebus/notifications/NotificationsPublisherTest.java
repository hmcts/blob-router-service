package uk.gov.hmcts.reform.blobrouter.servicebus.notifications;

import com.azure.messaging.servicebus.ServiceBusErrorSource;
import com.azure.messaging.servicebus.ServiceBusException;
import com.azure.messaging.servicebus.ServiceBusMessage;
import com.azure.messaging.servicebus.ServiceBusSenderClient;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.skyscreamer.jsonassert.JSONAssert;
import org.skyscreamer.jsonassert.JSONCompareMode;
import uk.gov.hmcts.reform.blobrouter.data.events.ErrorCode;
import uk.gov.hmcts.reform.blobrouter.servicebus.notifications.model.NotificationMsg;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class NotificationsPublisherTest {

    private NotificationsPublisher notifier;

    @Mock
    private ServiceBusSenderClient queueClient;

    @BeforeEach
    void setUp() {
        notifier = new NotificationsPublisher(queueClient, new ObjectMapper());
    }

    @Test
    void publish_should_send_message_with_right_content() throws Exception {
        // given
        NotificationMsg notificationMsg = new NotificationMsg(
            "test.zip",
            "C1",
            "1234",
            ErrorCode.ERR_SIG_VERIFY_FAILED,
            "Signature verification failed",
            "blob-router"
        );

        // when
        notifier.publish(notificationMsg, "messageId");

        // then
        ArgumentCaptor<ServiceBusMessage> messageCaptor = ArgumentCaptor.forClass(ServiceBusMessage.class);
        verify(queueClient).sendMessage(messageCaptor.capture());

        ServiceBusMessage message = messageCaptor.getValue();

        assertThat(message.getContentType()).isEqualTo("application/json");

        String messageBodyJson = message.getBody().toString();
        String expectedMessageBodyJson = String.format(
            "{\"zip_file_name\":\"%s\", \"container\":\"%s\", \"document_control_number\":\"%s\", "
                + "\"error_code\":\"%s\", \"error_description\":\"%s\", \"service\":\"%s\"}",
            notificationMsg.zipFileName,
            notificationMsg.container,
            notificationMsg.documentControlNumber,
            notificationMsg.errorCode,
            notificationMsg.errorDescription,
            notificationMsg.service
        );
        JSONAssert.assertEquals(expectedMessageBodyJson, messageBodyJson, JSONCompareMode.LENIENT);
        assertThat(message.getMessageId()).isEqualTo("messageId");
    }

    @Test
    void publish_should_throw_exception_when_queue_client_fails() throws Exception {
        // given
        NotificationMsg notificationMsg = new NotificationMsg(
            "A.zip", "C1", "123", null, "Xyz", "service1"
        );

        ServiceBusException exceptionToThrow = new ServiceBusException(
            new IllegalAccessError("test exception"),
            ServiceBusErrorSource.ABANDON
        );
        willThrow(exceptionToThrow)
            .given(queueClient)
            .sendMessage(any());

        // when
        Throwable exc = catchThrowable(
            () -> notifier.publish(notificationMsg, "id1")
        );

        // then
        assertThat(exc)
            .isInstanceOf(NotificationsPublishingException.class)
            .hasMessage(
                "An error occurred when trying to publish notification for "
                    + "File name: A.zip, Container: C1, Message Id: id1"
            ).hasCause(exceptionToThrow);
    }

}
