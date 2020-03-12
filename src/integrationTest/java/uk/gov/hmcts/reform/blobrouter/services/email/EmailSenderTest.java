package uk.gov.hmcts.reform.blobrouter.services.email;

import com.icegreen.greenmail.util.GreenMail;
import com.icegreen.greenmail.util.ServerSetup;
import com.icegreen.greenmail.util.ServerSetupTest;
import com.microsoft.applicationinsights.boot.dependencies.google.common.io.Resources;
import org.apache.commons.mail.util.MimeMessageParser;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.io.File;
import java.util.Map;
import javax.activation.DataSource;
import javax.mail.Address;

import static java.util.Collections.emptyMap;
import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
public class EmailSenderTest {

    // as defined in application.properties
    private static final String USERNAME = "test_username";
    private static final String PASSWORD = "test_password";

    private static final String FROM_ADDRESS = "a@b.c";
    private static final String SUBJECT = "subject";
    private static final String RECIPIENT_1 = "Foo <foo@hmcts.net>";
    private static final String RECIPIENT_2 = "bar@hmcts.net";
    private static final String[] RECIPIENTS_1 = {RECIPIENT_1, RECIPIENT_2};
    private static final String FILE_NAME_1 = "email/test1.zip";
    private static final String FILE_NAME_2 = "email/test2.zip";
    private static final String BODY = "body";

    @Autowired
    private EmailSender emailSender;

    private GreenMail smtpServer;

    @BeforeEach
    void setUp() {
        smtpServer = new GreenMail(
            new ServerSetup(
                ServerSetupTest.SMTP.getPort(),
                null,
                ServerSetupTest.SMTP.getProtocol()
            )
        );
        smtpServer.setUser(USERNAME, PASSWORD);
        smtpServer.start();
        smtpServer.getSmtp();
    }

    @AfterEach
    void tearDown() {
        smtpServer.stop();
    }

    @Test
    void should_send_email_to_all_recepients() throws Exception {
        File file1 = new File(Resources.getResource(FILE_NAME_1).toURI());
        File file2 = new File(Resources.getResource(FILE_NAME_2).toURI());

        emailSender.sendMessageWithAttachments(
            SUBJECT,
            BODY,
            FROM_ADDRESS,
            RECIPIENTS_1,
            Map.of(FILE_NAME_1, file1, FILE_NAME_2, file2)
        );

        assertThat(smtpServer.getReceivedMessages()).hasSize(RECIPIENTS_1.length);
        MimeMessageParser msg = new MimeMessageParser(smtpServer.getReceivedMessages()[0]).parse();
        assertThat(msg.getFrom()).isEqualTo(FROM_ADDRESS);
        assertThat(msg.getTo())
            .extracting(Address::toString)
            .hasSize(2)
            .containsExactly(
                RECIPIENT_1,
                RECIPIENT_2
            );
        assertThat(msg.getSubject()).isEqualTo(SUBJECT);
        assertThat(msg.getPlainContent()).isEqualTo(BODY);
        assertThat(msg.getAttachmentList()).hasSize(2);
        assertThat(msg.getAttachmentList().stream()
                       .map(DataSource::getName))
            .containsExactlyInAnyOrder(FILE_NAME_1, FILE_NAME_2);
    }

    @Test
    void should_handle_no_attachments() throws Exception {
        emailSender.sendMessageWithAttachments(
            SUBJECT,
            BODY,
            FROM_ADDRESS,
            RECIPIENTS_1,
            emptyMap()
        );

        assertThat(smtpServer.getReceivedMessages()).hasSize(RECIPIENTS_1.length);
        MimeMessageParser msg = new MimeMessageParser(smtpServer.getReceivedMessages()[0]).parse();
        assertThat(msg.getFrom()).isEqualTo(FROM_ADDRESS);
        assertThat(msg.getTo())
            .extracting(Address::toString)
            .hasSize(2)
            .containsExactly(
                RECIPIENT_1,
                RECIPIENT_2
            );
        assertThat(msg.getSubject()).isEqualTo(SUBJECT);
        assertThat(msg.getPlainContent()).isEqualTo(BODY);
        assertThat(msg.getAttachmentList()).isEmpty();
    }
}
