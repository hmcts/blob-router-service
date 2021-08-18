package uk.gov.hmcts.reform.blobrouter.config;

import com.azure.messaging.servicebus.ServiceBusSenderClient;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;

import static org.mockito.Mockito.mock;

public class QueueClientConfig {

    @Bean
    @ConditionalOnProperty(name = "queue.notifications.access-key", havingValue = "false")
    public ServiceBusSenderClient notificationsQueueClient() {
        return mock(ServiceBusSenderClient.class);
    }
}
