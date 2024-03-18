package uk.gov.hmcts.reform.blobrouter.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import uk.gov.hmcts.reform.blobrouter.services.email.MessageSender;

import java.io.File;
import java.util.Map;

/**
 * The `EmailConfig` class in Java creates an email sender bean that logs a message and does nothing if the SMTP
 * configuration is disabled.
 */
@Configuration
public class EmailConfig {

    /**
     * This function creates an email sender that logs a message and does nothing if the SMTP configuration is disabled.
     *
     * @return A `MessageSender` bean is being returned. This bean is a custom implementation of `MessageSender`
     *      interface that logs a message indicating that the email sending functionality is disabled due to the
     *      SMTP configuration being disabled.
     */
    @Bean
    @ConditionalOnProperty(name = "spring.mail.host", havingValue = "false")
    public MessageSender emailSenderDisabled() {
        return new MessageSender() {
            final Logger log = LoggerFactory.getLogger(MessageSender.class);

            @Override
            public void sendMessageWithAttachments(
                String subject,
                String body,
                String from,
                String[] recipients,
                Map<String, File> attachments
            ) {
                log.info("Not sending the email because the SMTP config is disabled.");
                // do nothing
            }
        };
    }
}
