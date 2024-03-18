package uk.gov.hmcts.reform.blobrouter.config;

import com.azure.messaging.servicebus.ServiceBusSenderClient;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;

import static org.mockito.Mockito.mock;

/**
 * The `QueueClientConfig` class creates a bean for a `ServiceBusSenderClient`
 * only if a specific property is set to false,
 * returning a mock `ServiceBusSenderClient`.
 */
public class QueueClientConfig {

    /**
     * This function creates a bean for a ServiceBusSenderClient only if a specific property is set to false.
     *
     * @return A mock `ServiceBusSenderClient` is being returned.
     */
    @Bean
    @ConditionalOnProperty(name = "queue.notifications.access-key", havingValue = "false")
    public ServiceBusSenderClient notificationsQueueClient() {
        return mock(ServiceBusSenderClient.class);
    }
}
