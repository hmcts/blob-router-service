package uk.gov.hmcts.reform.blobrouter.servicebus.notifications;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.microsoft.azure.servicebus.Message;
import com.microsoft.azure.servicebus.MessageBody;
import com.microsoft.azure.servicebus.QueueClient;
import com.microsoft.azure.servicebus.primitives.ServiceBusException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.skyscreamer.jsonassert.JSONAssert;
import org.skyscreamer.jsonassert.JSONCompareMode;
import org.springframework.util.CollectionUtils;
import uk.gov.hmcts.reform.blobrouter.servicebus.notifications.model.NotificationMsg;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class NotificationsPublisherTest {

    private NotificationsPublisher notifier;

    @Mock
    private QueueClient queueClient;

    @BeforeEach
    void setUp() {
        notifier = new NotificationsPublisher(queueClient, new ObjectMapper());
    }

    @Test
    void publish_should_send_message_with_right_content() throws Exception {
        // given
        NotificationMsg notificationMsg = new NotificationMsg(
            "test.zip",
            "ABC",
            "PO123",
            "1234",
            "Invalid Signature",
            "Signature verification failed",
            "blob-router"
        );

        // when
        notifier.publish(notificationMsg);

        // then
        ArgumentCaptor<Message> messageCaptor = ArgumentCaptor.forClass(Message.class);
        verify(queueClient).send(messageCaptor.capture());

        Message message = messageCaptor.getValue();

        assertThat(message.getContentType()).isEqualTo("application/json");

        String messageBodyJson = new String(getBinaryData(message.getMessageBody()));
        String expectedMessageBodyJson = String.format(
            "{\"zip_file_name\":\"%s\", \"jurisdiction\":\"%s\", \"po_box\":\"%s\", "
                + "\"document_control_number\":\"%s\", \"error_code\":\"%s\", \"error_description\":\"%s\", "
                + "\"service\":\"%s\"}",
            notificationMsg.zipFileName,
            notificationMsg.jurisdiction,
            notificationMsg.poBox,
            notificationMsg.documentControlNumber,
            notificationMsg.errorCode,
            notificationMsg.errorDescription,
            notificationMsg.service
        );
        JSONAssert.assertEquals(expectedMessageBodyJson, messageBodyJson, JSONCompareMode.LENIENT);
    }

    @Test
    void publish_should_throw_exception_when_queue_client_fails() throws Exception {
        // given
        NotificationMsg notificationMsg = new NotificationMsg(
            "A.zip", "TEST", "123", null, "Xyz", "error", "service1"
        );

        ServiceBusException exceptionToThrow = new ServiceBusException(true, "test exception");
        willThrow(exceptionToThrow)
            .given(queueClient)
            .send(any());

        // when
        Throwable exc = catchThrowable(
            () -> notifier.publish(notificationMsg)
        );

        // then
        assertThat(exc)
            .isInstanceOf(NotificationsPublishingException.class)
            .hasMessage(
                "An error occurred when trying to publish notification for "
                    + "Blob name: A.zip, Jurisdiction: TEST, PO Box: 123"
            ).hasCause(exceptionToThrow);
    }

    private byte[] getBinaryData(MessageBody messageBody) {
        List<byte[]> binaryData = messageBody.getBinaryData();

        return CollectionUtils.isEmpty(binaryData) ? null : binaryData.get(0);
    }
}
