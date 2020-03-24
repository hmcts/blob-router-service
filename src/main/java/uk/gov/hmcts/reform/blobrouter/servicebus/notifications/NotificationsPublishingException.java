package uk.gov.hmcts.reform.blobrouter.servicebus.notifications;

public class NotificationsPublishingException extends RuntimeException {
    private static final long serialVersionUID = -1971985358931615643L;

    public NotificationsPublishingException(String message, Throwable cause) {
        super(message, cause);
    }
}
