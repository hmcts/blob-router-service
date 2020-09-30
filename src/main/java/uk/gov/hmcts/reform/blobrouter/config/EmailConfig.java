package uk.gov.hmcts.reform.blobrouter.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;
import uk.gov.hmcts.reform.blobrouter.services.email.MessageSender;

import java.io.File;
import java.util.Map;

@Configuration
@Lazy
public class EmailConfig {

    @Bean
    @ConditionalOnProperty(name = "spring.mail.host", havingValue = "false")
    public MessageSender emailSenderDisabled() {
        return new MessageSender() {
            @Override
            public void sendMessageWithAttachments(
                String subject,
                String body,
                String from,
                String[] recipients,
                Map<String, File> attachments
            ) {
                // do nothing
            }
        };
    }

}
