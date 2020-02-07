package uk.gov.hmcts.reform.blobrouter.services.email;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Component;

import java.io.File;
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

    public void sendMessageWithAttachment(
        String subj,
        String body,
        String from,
        String[] recipients,
        String fileName,
        File file
    ) throws SendEmailException {
        try {
            MimeMessage msg = mailSender.createMimeMessage();

            MimeMessageHelper helper = new MimeMessageHelper(msg, true);
            helper.setFrom(from);
            helper.setTo(recipients);
            helper.setSubject(subj);
            helper.setText(body);
            helper.addAttachment(fileName, file);

            mailSender.send(msg);
        } catch (Exception exc) {
            throw new SendEmailException("Error sending message", exc);
        }
    }
}
