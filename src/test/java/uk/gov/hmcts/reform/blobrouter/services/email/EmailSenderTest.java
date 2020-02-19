package uk.gov.hmcts.reform.blobrouter.services.email;

import com.microsoft.applicationinsights.boot.dependencies.google.common.io.Resources;
import org.junit.jupiter.api.Test;
import org.springframework.mail.MailSendException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.JavaMailSenderImpl;

import java.io.File;
import java.util.Map;
import javax.mail.internet.MimeMessage;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowableOfType;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.mock;

public class EmailSenderTest {

    private static final String FROM_ADDRESS = "from@hmcts.net";
    private static final String RECIPIENT_1 = "Foo <foo@hmcts.net>";
    private static final String RECIPIENT_2 = "bar@hmcts.net";
    private static final String FILE_NAME_1 = "email/test1.zip";
    private static final String FILE_NAME_2 = "email/test2.zip";
    private static final String SUBJECT = "subject";
    private static final String BODY = "body";

    @Test
    void should_handle_mail_exception() throws Exception {
        // given
        JavaMailSender mailSender = mock(JavaMailSender.class);

        given(mailSender.createMimeMessage())
            .willReturn(new JavaMailSenderImpl().createMimeMessage());

        willThrow(MailSendException.class)
            .given(mailSender)
            .send(any(MimeMessage.class));

        EmailSender emailSender = new EmailSender(mailSender);

        File file1 = new File(Resources.getResource(FILE_NAME_1).toURI());
        File file2 = new File(Resources.getResource(FILE_NAME_2).toURI());

        // when
        SendEmailException ex = catchThrowableOfType(() -> emailSender.sendMessageWithAttachments(
            SUBJECT,
            BODY,
            FROM_ADDRESS,
            new String[]{RECIPIENT_1, RECIPIENT_2},
            Map.of(FILE_NAME_1, file1, FILE_NAME_2, file2)
        ), SendEmailException.class);

        // then
        assertThat(ex.getMessage())
            .isEqualTo(String.format("Error sending message, subject %s", SUBJECT));
    }
}
