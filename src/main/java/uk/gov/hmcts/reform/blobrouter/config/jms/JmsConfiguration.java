package uk.gov.hmcts.reform.blobrouter.config.jms;

import org.apache.qpid.jms.JmsConnectionFactory;
import org.apache.qpid.jms.policy.JmsDefaultRedeliveryPolicy;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jms.annotation.EnableJms;
import org.springframework.jms.connection.CachingConnectionFactory;
import org.springframework.jms.core.JmsTemplate;

import javax.jms.ConnectionFactory;

@Configuration
@EnableJms
@ConditionalOnProperty(name = "jms.enabled", havingValue = "true")
public class JmsConfiguration {

    @Value("${jms.namespace}")
    private String namespace;

    @Value("${jms.username}")
    private String username;

    @Value("${jms.password}")
    private String password;

    @Value("${jms.receiveTimeout}")
    private Long receiveTimeout;

    @Value("${jms.idleTimeout}")
    private Long idleTimeout;

    @Value("${jms.amqp-connection-string-template}")
    public String amqpConnectionStringTemplate;

    @Value("${jms.application-name}")
    public String clientId;

    /**
     * The function creates and configures a JMS ConnectionFactory bean in Java with specified properties.
     *
     * @param clientId The `clientId` parameter in the `connectionFactory`
     *      method is used to set the client ID for the JMS
     *      connection. This client ID is typically used to uniquely identify a client connection
     *      when connecting to a JMS provider. It helps in distinguishing different clients that are connected to
     *      the same JMS provider
     * @return A `CachingConnectionFactory` is being returned from the `connectionFactory` method.
     */
    @Bean
    public ConnectionFactory connectionFactory(@Value("${jms.application-name}") final String clientId) {
        String connection = String.format(amqpConnectionStringTemplate, namespace, idleTimeout);
        JmsConnectionFactory jmsConnectionFactory = new JmsConnectionFactory(connection);
        jmsConnectionFactory.setUsername(username);
        jmsConnectionFactory.setPassword(password);
        JmsDefaultRedeliveryPolicy jmsDefaultRedeliveryPolicy = new JmsDefaultRedeliveryPolicy();
        jmsDefaultRedeliveryPolicy.setMaxRedeliveries(3);
        jmsConnectionFactory.setRedeliveryPolicy(jmsDefaultRedeliveryPolicy);
        jmsConnectionFactory.setClientID(clientId);
        return new CachingConnectionFactory(jmsConnectionFactory);
    }

    /**
     * This function creates a JmsTemplate bean with a specified connection factory, default destination name,
     * and receive timeout.
     *
     * @param connectionFactory The `connectionFactory` parameter in the `jmsTemplate` method is an instance of
     *      `javax.jms.ConnectionFactory`. It is used to create connections to the JMS provider, which is
     *      necessary for sending and receiving messages to and from a JMS destination.
     *      The `connectionFactory` is typically configured
     * @return A JmsTemplate bean is being returned with the specified configuration settings, including the connection
     *      factory, default destination name, and receive timeout of 5 seconds.
     */
    @Bean
    public JmsTemplate jmsTemplate(ConnectionFactory connectionFactory) {
        JmsTemplate jmsTemplate = new JmsTemplate();
        jmsTemplate.setConnectionFactory(connectionFactory);
        jmsTemplate.setDefaultDestinationName("notifications");
        jmsTemplate.setReceiveTimeout(5000); // Set the receive timeout to 5 seconds
        return jmsTemplate;
    }
}
