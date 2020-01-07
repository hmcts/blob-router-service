package uk.gov.hmcts.reform.blobrouter.clients;

import com.fasterxml.jackson.annotation.JsonProperty;

public class ErrorNotificationResponse {

    private final String notificationId;

    public ErrorNotificationResponse(@JsonProperty("notification_id") String notificationId) {
        this.notificationId = notificationId;
    }

    public String getNotificationId() {
        return notificationId;
    }
}
