package uk.gov.hmcts.reform.blobrouter.services.email;

import com.icegreen.greenmail.util.ServerSetupTest;
import com.microsoft.applicationinsights.boot.dependencies.google.common.io.Resources;
import org.apache.commons.mail.util.MimeMessageParser;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.MailSendException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import uk.gov.hmcts.reform.blobrouter.jupiter.GreenMailExtension;

import java.io.File;
import java.util.Properties;
import javax.mail.Address;
import javax.mail.internet.MimeMessage;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowableOfType;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.mock;

@ExtendWith(MockitoExtension.class)
public class EmailSenderTest {

    private static final String TEST_LOGIN = "test@localhost.com";
    private static final String TEST_PASSWORD = "test_password";
    private static final String FROM_ADDRESS = "from@hmcts.net";
    private static final String RECIPIENT_1 = "Foo <foo@hmcts.net>";
    private static final String RECIPIENT_2 = "bar@hmcts.net";
    public static final String FILE_NAME = "test.zip";
    public static final String SUBJ = "subj";
    public static final String BODY = "body";

    @RegisterExtension
    static GreenMailExtension greenMail = new GreenMailExtension(ServerSetupTest.SMTP);

    @Test
    public void should_send_email_to_all_recipients() throws Exception {
        // given

        greenMail.setUser(TEST_LOGIN, TEST_PASSWORD);
        EmailSender emailSender = new EmailSender(getMailSender());

        File file = new File(Resources.getResource(FILE_NAME).toURI());

        // when
        emailSender.sendMessageWithAttachment(
            SUBJ,
            BODY,
            FROM_ADDRESS,
            new String[]{RECIPIENT_1, RECIPIENT_2},
            FILE_NAME,
            file
        );

        // then
        MimeMessageParser msg = new MimeMessageParser(greenMail.getReceivedMessages()[0]).parse();

        assertThat(msg.getTo())
            .extracting(Address::toString)
            .hasSize(2)
            .containsExactly(
                RECIPIENT_1,
                RECIPIENT_2
            );
        assertThat(msg.getSubject()).isEqualTo(SUBJ);
        assertThat(msg.getPlainContent()).isEqualTo(BODY);
        assertThat(msg.getAttachmentList()).hasSize(1);
        assertThat(msg.getAttachmentList().get(0).getName()).isEqualTo(FILE_NAME);
    }

    @Test
    public void should_handle_mail_exception() throws Exception {
        // given
        JavaMailSender mailSender = mock(JavaMailSender.class);

        given(mailSender.createMimeMessage())
            .willReturn(new JavaMailSenderImpl().createMimeMessage());

        willThrow(MailSendException.class)
            .given(mailSender)
            .send(any(MimeMessage.class));

        EmailSender emailSender = new EmailSender(mailSender);

        File file = new File(Resources.getResource(FILE_NAME).toURI());

        // when
        SendEmailException ex = catchThrowableOfType(() -> emailSender.sendMessageWithAttachment(
            SUBJ,
            BODY,
            FROM_ADDRESS,
            new String[]{RECIPIENT_1, RECIPIENT_2},
            FILE_NAME,
            file
        ), SendEmailException.class);

        // then
        assertThat(ex.getMessage()).isEqualTo("Error sending message");
    }

    @SuppressWarnings("PMD.LawOfDemeter")
    private JavaMailSender getMailSender() {
        JavaMailSenderImpl mailSender = new JavaMailSenderImpl();
        mailSender.setHost("localhost");
        mailSender.setPort(ServerSetupTest.SMTP.getPort());
        mailSender.setUsername(TEST_LOGIN);
        mailSender.setPassword(TEST_PASSWORD);

        Properties props = mailSender.getJavaMailProperties();
        props.put("mail.transport.protocol", ServerSetupTest.SMTP.getProtocol());

        return mailSender;
    }
}
