package uk.gov.hmcts.reform.blobrouter.services.email;

import jakarta.mail.internet.MimeMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Component;

import java.io.File;
import java.util.Map;

/**
 * The `EmailSender` class in Java is a component that implements `MessageSender` interface and provides a method
 * `sendMessageWithAttachments` to send emails with attachments using Java Mail API.
 */
@Component
@ConditionalOnProperty("spring.mail.host")
public class EmailSender implements MessageSender {

    private static final Logger log = LoggerFactory.getLogger(EmailSender.class);

    private final JavaMailSender mailSender;

    // region constructor
    public EmailSender(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }
    // endregion

    /**
     * The function `sendMessageWithAttachments` sends an email with attachments using Java Mail API.
     *
     * @param subject The `subject` parameter is a String that represents the subject of the email you want to send. It
     *                typically describes the main topic or purpose of the email.
     * @param body The `body` parameter in the `sendMessageWithAttachments` method refers to the main content or text of
     *             the email message that you want to send. It typically contains the message you want to
     *             convey to the recipients. You can include any relevant information, greetings,
     *             instructions, or details in the email body.
     * @param from The `from` parameter in the `sendMessageWithAttachments` method represents the email
     *             address of the sender. It specifies the email address from which the email will be sent.
     * @param recipients The `recipients` parameter in the `sendMessageWithAttachments` method is an array
     *                   of Strings that contains the email addresses of the recipients to whom the email
     *                   will be sent. You can specify one or more email addresses in this array to send
     *                   the email to multiple recipients.
     * @param attachments The `sendMessageWithAttachments` method you provided is used to send an email
     *                    with attachments. The `attachments` parameter is a map of String and File where the
     *                    key is the name of the attachment and the value is the corresponding `File` object
     *                    representing the attachment file.
     */
    @Override
    public void sendMessageWithAttachments(
        String subject,
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
            helper.setSubject(subject);
            helper.setText(body);
            for (Map.Entry<String, File> attachment : attachments.entrySet()) {
                helper.addAttachment(attachment.getKey(), attachment.getValue());
            }

            mailSender.send(msg);

            log.info(String.format("Message sent, subject %s", subject));
        } catch (Exception exc) {
            throw new SendEmailException(
                String.format("Error sending message, subject %s", subject),
                exc
            );
        }
    }
}
