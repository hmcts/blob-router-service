package uk.gov.hmcts.reform.blobrouter.services.email;

import org.junit.jupiter.api.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.context.junit4.SpringRunner;

import javax.mail.internet.MimeMessage;

import static java.util.Collections.emptyMap;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;

@SpringBootTest
@RunWith(SpringRunner.class)
public class EmailSenderIntegrationTest {

    @Autowired
    private EmailSender emailSender;

    @SpyBean
    private JavaMailSender mailSender;

    @Test
    public void should_attempt_to_send_report_when_recipients_list_is_present() {
        try {
            emailSender.sendMessageWithAttachments("subject", "body", "a@b.c", new String[]{"d@e.f"}, emptyMap());
        } catch (SendEmailException ex) {
            // SMTP server is not running so ignore the exception and just verify the method is called
            verify(mailSender).send(any(MimeMessage.class));
        }
    }
}
