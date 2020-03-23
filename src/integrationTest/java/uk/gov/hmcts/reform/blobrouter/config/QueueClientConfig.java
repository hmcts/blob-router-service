package uk.gov.hmcts.reform.blobrouter.config;

import com.microsoft.azure.servicebus.QueueClient;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;

import static org.mockito.Mockito.mock;

public class QueueClientConfig {

    @Bean
    @ConditionalOnProperty(name = "queue.notifications.connection-string", havingValue = "false")
    public QueueClient notificationsQueueClient() {
        return mock(QueueClient.class);
    }
}
