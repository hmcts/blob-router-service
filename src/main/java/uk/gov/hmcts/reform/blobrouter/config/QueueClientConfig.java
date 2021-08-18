package uk.gov.hmcts.reform.blobrouter.config;

import com.azure.messaging.servicebus.ServiceBusClientBuilder;
import com.azure.messaging.servicebus.ServiceBusSenderClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class QueueClientConfig {

    @Bean
    @ConditionalOnProperty("queue.notifications.access-key")
    public ServiceBusSenderClient notificationsQueueClient(
        @Value("${queue.notifications.access-key}") String accessKey,
        @Value("${queue.notifications.access-key-name}") String accessKeyName,
        @Value("${queue.notifications.namespace}") String namespace,
        @Value("${queue.notifications.queue-name}") String queueName
    ) {
        String connectionString = String.format(
            "Endpoint=sb://%s.servicebus.windows.net;SharedAccessKeyName=%s;SharedAccessKey=%s;",
            namespace,
            accessKeyName,
            accessKey
        );

        return new ServiceBusClientBuilder()
            .connectionString(connectionString)
            .sender()
            .queueName(queueName)
            .buildClient();

    }


}
