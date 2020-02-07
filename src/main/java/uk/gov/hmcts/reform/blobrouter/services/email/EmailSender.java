package uk.gov.hmcts.reform.blobrouter.services.email;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Component;

import java.io.File;
import java.util.Map;
import javax.mail.internet.MimeMessage;

@Component
@ConditionalOnProperty(prefix = "spring.mail", name = "host")
public class EmailSender {

    private static final Logger log = LoggerFactory.getLogger(EmailSender.class);

    private final JavaMailSender mailSender;

    // region constructor
    public EmailSender(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }
    // endregion

    public void sendMessageWithAttachments(
        String subj,
        String body,
        String from,
        String[] recipients,
        Map<String, File> attachments
    ) throws SendEmailException {
        try {
            MimeMessage msg = mailSender.createMimeMessage();

            MimeMessageHelper helper = new MimeMessageHelper(msg, true);
            helper.setFrom(from);
            helper.setTo(recipients);
            helper.setSubject(subj);
            helper.setText(body);
            for (Map.Entry<String, File> attachment : attachments.entrySet()) {
                helper.addAttachment(attachment.getKey(), attachment.getValue());
            }

            mailSender.send(msg);
        } catch (Exception exc) {
            throw new SendEmailException("Error sending message", exc);
        }
    }
}
