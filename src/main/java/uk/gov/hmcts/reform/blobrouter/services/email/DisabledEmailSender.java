package uk.gov.hmcts.reform.blobrouter.services.email;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.io.File;
import java.util.Map;

@Component
@ConditionalOnProperty(value = "spring.mail.host", havingValue = "false")
public class DisabledEmailSender implements MessageSender {

    private static final Logger log = LoggerFactory.getLogger(DisabledEmailSender.class);

    @Override
    public void sendMessageWithAttachments(
        String subject,
        String body,
        String from,
        String[] recipients,
        Map<String, File> attachments
    ) {
        log.info("Not sending email because the SMTP config is disabled.");
    }
}
