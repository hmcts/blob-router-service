package uk.gov.hmcts.reform.blobrouter.config;

import com.microsoft.azure.servicebus.QueueClient;
import com.microsoft.azure.servicebus.ReceiveMode;
import com.microsoft.azure.servicebus.primitives.ConnectionStringBuilder;
import com.microsoft.azure.servicebus.primitives.ServiceBusException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class QueueClientConfig {

    @Bean
    @ConditionalOnProperty("queue.notifications.access-key")
    public QueueClient notificationsQueueClient(
        @Value("${queue.notifications.access-key}") String accessKey,
        @Value("${queue.notifications.access-key-name}") String accessKeyName,
        @Value("${queue.notifications.namespace}") String namespace
    ) throws InterruptedException, ServiceBusException {
        return new QueueClient(
            new ConnectionStringBuilder(
                namespace,
                "notifications",
                accessKeyName,
                accessKey
            ),
            ReceiveMode.PEEKLOCK
        );
    }

}
