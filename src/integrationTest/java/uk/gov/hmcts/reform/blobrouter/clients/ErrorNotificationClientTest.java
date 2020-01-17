package uk.gov.hmcts.reform.blobrouter.clients;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.tomakehurst.wiremock.client.ResponseDefinitionBuilder;
import feign.FeignException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.contract.wiremock.AutoConfigureWireMock;
import org.springframework.test.context.ActiveProfiles;
import uk.gov.hmcts.reform.blobrouter.clients.errornotifications.ErrorNotificationClient;
import uk.gov.hmcts.reform.blobrouter.clients.errornotifications.ErrorNotificationRequest;
import uk.gov.hmcts.reform.blobrouter.clients.errornotifications.ErrorNotificationResponse;

import static com.github.tomakehurst.wiremock.client.WireMock.created;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.unauthorized;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

@AutoConfigureWireMock(port = 0)
@ActiveProfiles("integration-test")
@SpringBootTest(properties = "clients.error-notifications.url=http://localhost:${wiremock.server.port}")
public class ErrorNotificationClientTest {

    private static final ErrorNotificationRequest TEST_NOTIFICATION_REQUEST = new ErrorNotificationRequest(
        "test_zip_file_name",
        "test_po_box",
        "test_error_code",
        "test_error_description"
    );

    private static final ErrorNotificationResponse TEST_NOTIFICATION_RESPONSE =
        new ErrorNotificationResponse("some id");

    @Autowired
    private ErrorNotificationClient client;

    @Autowired
    private ObjectMapper mapper;

    @Test
    public void should_return_Created_when_everything_is_ok_with_request() throws JsonProcessingException {
        // given
        stubWithResponse(created().withBody(mapper.writeValueAsBytes(TEST_NOTIFICATION_RESPONSE)));

        // when
        ErrorNotificationResponse notificationResponse = client.notify(TEST_NOTIFICATION_REQUEST);

        // then
        assertThat(notificationResponse).isEqualToComparingFieldByField(TEST_NOTIFICATION_RESPONSE);
    }

    @Test
    public void should_return_NotificationClientException_when_badly_authorised() {
        // given
        stubWithResponse(unauthorized());

        // when
        Throwable throwable = catchThrowable(() -> client.notify(TEST_NOTIFICATION_REQUEST));

        // then
        assertThat(throwable).isInstanceOf(FeignException.Unauthorized.class);
    }

    private void stubWithResponse(ResponseDefinitionBuilder builder) {
        stubFor(post("/notifications").willReturn(builder));
    }
}
