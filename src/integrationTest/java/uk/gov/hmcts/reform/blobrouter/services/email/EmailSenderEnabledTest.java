package uk.gov.hmcts.reform.blobrouter.services.email;

import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.mail.javamail.JavaMailSender;

import javax.mail.internet.MimeMessage;

import static java.util.Collections.emptyMap;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;

@SpringBootTest
public class ReportSenderTest {

    @Autowired
    private EmailSender emailSender;

    @SpyBean
    private JavaMailSender mailSender;

    @Test
    public void should_attempt_to_send_report_when_recipients_list_is_present() throws Exception {
        emailSender.sendMessageWithAttachments("subject", "body", "a@b.c", new String[]{"e@f.g"}, emptyMap());

        verify(mailSender).send(any(MimeMessage.class));
    }
}
