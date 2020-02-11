package uk.gov.hmcts.reform.blobrouter.services.email;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import static java.util.Collections.emptyMap;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowableOfType;

@SpringBootTest
@ActiveProfiles("db-test")
public class EmailSenderIntegrationTest {

    private static final String FROM_ADDRESS = "a@b.c";
    private static final String SUBJECT = "subject";

    @Autowired
    private EmailSender emailSender;

    @Test
    public void should_attempt_to_send_email() {

        SendEmailException ex =
            catchThrowableOfType(
                () -> emailSender.sendMessageWithAttachments(
                    SUBJECT,
                    "body",
                    FROM_ADDRESS,
                    new String[]{"d@e.f"},
                    emptyMap()
                ),
                SendEmailException.class
            );

        // SMTP server is not running so exception is thrown, this indicates that the method has been called
        assertThat(ex.getMessage())
            .isEqualTo(
                String.format(
                    "Error sending message, from %s, subject %s",
                    FROM_ADDRESS,
                    SUBJECT
                )
            );
    }
}
