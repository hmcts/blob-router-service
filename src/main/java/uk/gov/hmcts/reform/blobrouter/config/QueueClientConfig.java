package uk.gov.hmcts.reform.blobrouter.config;

import com.azure.messaging.servicebus.ServiceBusClientBuilder;
import com.azure.messaging.servicebus.ServiceBusSenderClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * The `QueueClientConfig` class in Java configures a ServiceBusSenderClient bean for sending messages to an
 * Azure Service Bus queue based on provided configuration properties.
 */
@Configuration
@ConditionalOnExpression("!${jms.enabled}")
public class QueueClientConfig {

    /**
     * The function creates a ServiceBusSenderClient for sending messages to a queue based on the provided configuration
     * properties.
     *
     * @param accessKey The `accessKey` parameter in the code snippet represents the access key required to authenticate
     *      and authorize access to the Azure Service Bus queue. This key is used along with the access key name,
     *      namespace, and queue name to construct the connection string for the Service Bus client. It is a sensitive
     *      piece of
     * @param accessKeyName The `accessKeyName` parameter in the code snippet refers to the name of the Shared Access
     *      Key that is used for authentication when connecting to the Azure Service Bus. This key is associated with
     *      a specific namespace and is required for establishing a secure connection to the Service Bus queue.
     * @param namespace The `namespace` parameter in the code snippet refers to the Azure Service Bus namespace where
     *      the queue is located. It typically follows the format `your-namespace.servicebus.windows.net`.
     *      This namespace is used to construct the connection string for the Service Bus client to connect to the
     *      specified queue for sending messages
     * @param queueName The `queueName` parameter in the code snippet refers to the name of the Azure Service Bus
     *      queue to which messages will be sent. This parameter is used to specify the destination queue for the
     *      `ServiceBusSenderClient` that is being created in the `notificationsQueueClient` bean.
     * @return A ServiceBusSenderClient object is being returned.
     */
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
