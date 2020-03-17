package uk.gov.hmcts.reform.blobrouter.services.email;

import com.icegreen.greenmail.util.GreenMail;
import com.icegreen.greenmail.util.GreenMailUtil;
import com.icegreen.greenmail.util.ServerSetup;
import com.icegreen.greenmail.util.ServerSetupTest;
import com.microsoft.applicationinsights.boot.dependencies.google.common.io.Resources;
import org.apache.commons.io.IOUtils;
import org.apache.commons.mail.util.MimeMessageParser;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.io.File;
import java.io.InputStream;
import java.util.Map;
import java.util.stream.Collectors;
import javax.mail.Address;
import javax.mail.MessagingException;
import javax.mail.Multipart;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMessage;

import static java.util.Arrays.asList;
import static java.util.Collections.emptyMap;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.http.MediaType.MULTIPART_MIXED_VALUE;

@SpringBootTest
@Disabled
public class EmailSenderTest {

    // as defined in application.properties
    private static final String USERNAME = "test_username";
    private static final String PASSWORD = "test_password";

    private static final String FROM_ADDRESS = "a@b.c";
    private static final String SUBJECT = "subject";
    private static final String RECIPIENT_1 = "Foo <foo@hmcts.net>";
    private static final String RECIPIENT_2 = "bar@hmcts.net";
    private static final String[] RECIPIENTS = {RECIPIENT_1, RECIPIENT_2};
    private static final String EMAIL_DIR = "email/";
    private static final String FILE_NAME_1 = "test1.zip";
    private static final String FILE_NAME_2 = "test2.zip";
    private static final String BODY = "message body";

    private static final String APPLICATION_ZIP = "application/zip";
    private static final String MULTIPART_RELATED_VALUE = "multipart/related";

    @Autowired
    private EmailSender emailSender;

    private GreenMail greenMail;

    @BeforeEach
    void setUp() {
        greenMail = new GreenMail(
            new ServerSetup(
                ServerSetupTest.SMTP.getPort(),
                null,
                ServerSetupTest.SMTP.getProtocol()
            )
        );
        greenMail.setUser(USERNAME, PASSWORD);
        greenMail.start();
        greenMail.getSmtp();
    }

    @AfterEach
    void tearDown() {
        greenMail.stop();
    }

    @Test
    void should_send_email_to_all_recepients() throws Exception {
        File file1 = new File(Resources.getResource(EMAIL_DIR + FILE_NAME_1).toURI());
        File file2 = new File(Resources.getResource(EMAIL_DIR + FILE_NAME_2).toURI());

        emailSender.sendMessageWithAttachments(
            SUBJECT,
            BODY,
            FROM_ADDRESS,
            RECIPIENTS,
            Map.of(FILE_NAME_1, file1, FILE_NAME_2, file2)
        );

        greenMail.waitForIncomingEmail(RECIPIENTS.length);

        assertThat(greenMail.getReceivedMessages()).hasSize(RECIPIENTS.length);
        MimeMessage msg = greenMail.getReceivedMessages()[0];
        String headers = GreenMailUtil.getHeaders(msg);
        assertThat(headers.contains("From: " + FROM_ADDRESS)).isTrue();
        assertThat(headers.contains("To: " + RECIPIENT_1 + ", " + RECIPIENT_2)).isTrue();
        assertThat(headers.contains("Subject: " + SUBJECT)).isTrue();
        assertThat(msg.getContentType().startsWith(MULTIPART_MIXED_VALUE)).isTrue();
        Multipart multipartReceived = (Multipart) msg.getContent();

        assertThat(multipartReceived.getCount()).isEqualTo(3);

        MimeBodyPart msgTextPart = (MimeBodyPart) multipartReceived.getBodyPart(0);
        String msgTextContentType = msgTextPart.getContentType();
        assertThat(msgTextContentType.contains(MULTIPART_RELATED_VALUE)).isTrue();
        InputStream msgTextContent = msgTextPart.getInputStream();
        byte[] msgTextData = IOUtils.toByteArray(msgTextContent);
        String text = new String(msgTextData);
        assertThat(text.contains(BODY)).isTrue();

        MimeBodyPart msgFilePart1 = (MimeBodyPart) multipartReceived.getBodyPart(1);
        MimeBodyPart msgFilePart2 = (MimeBodyPart) multipartReceived.getBodyPart(2);
        assertThat(asList(msgFilePart1, msgFilePart2)
            .stream()
            .map(p -> {
                try {
                    return p.getContentType();
                } catch (MessagingException ex) {
                    throw new RuntimeException(ex);
                }
            })
            .collect(Collectors.toList()))
            .containsExactlyInAnyOrder(
                getContentType(FILE_NAME_1),
                getContentType(FILE_NAME_2)
            );
    }

    @Test
    void should_handle_no_attachments() throws Exception {
        emailSender.sendMessageWithAttachments(
            SUBJECT,
            BODY,
            FROM_ADDRESS,
            RECIPIENTS,
            emptyMap()
        );

        MimeMessageParser msg = new MimeMessageParser(greenMail.getReceivedMessages()[0]).parse();

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

    private String getContentType(String fileName) {
        return APPLICATION_ZIP + "; name=" + fileName;
    }
}
