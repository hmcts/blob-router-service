package uk.gov.hmcts.reform.blobrouter.services.email;

import org.junit.jupiter.api.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.context.junit4.SpringRunner;

import javax.mail.internet.MimeMessage;

import static java.util.Collections.emptyMap;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;

@SpringBootTest
@RunWith(SpringRunner.class)
public class EmailSenderIntegrationTest {

    private static final String FROM_ADDRESS = "a@b.c";
    private static final String TO_ADDRESS = "d@e.f";
    private static final String SUBJECT = "subject";

    @Autowired
    private EmailSender emailSender;

    @SpyBean
    private JavaMailSender mailSender;

    @Test
    public void should_attempt_to_send_report_when_recipients_list_is_present() throws Exception {
        ArgumentCaptor<MimeMessage> messageCaptor = ArgumentCaptor.forClass(MimeMessage.class);

        try {
            emailSender.sendMessageWithAttachments(SUBJECT, "body", FROM_ADDRESS, new String[]{TO_ADDRESS}, emptyMap());
        } catch (SendEmailException ex) {
            // SMTP server is not running so ignore the exception and just verify the method is called
        }

        verify(mailSender).send(messageCaptor.capture());
        MimeMessage message = messageCaptor.getValue();
        assertThat(message.getHeader("From")[0]).isEqualTo(FROM_ADDRESS);
        assertThat(message.getHeader("To")[0]).isEqualTo(TO_ADDRESS);
        assertThat(message.getHeader("Subject")[0]).isEqualTo(SUBJECT);
    }
}
