package uk.gov.hmcts.reform.blobrouter.servicebus.notifications;

import uk.gov.hmcts.reform.blobrouter.servicebus.notifications.model.NotificationMsg;

public interface INotificationsPublisher {

    void notify(NotificationMsg notificationMsg);

}
