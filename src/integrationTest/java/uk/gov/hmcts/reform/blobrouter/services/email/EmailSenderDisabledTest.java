package uk.gov.hmcts.reform.blobrouter.services.email;

import org.junit.jupiter.api.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringRunner;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@TestPropertySource(
    properties = {
        "spring.mail.host=false"
    }
)
@RunWith(SpringRunner.class)
public class EmailSenderDisabledTest {

    @Autowired
    private ApplicationContext context;

    @Test
    public void should_not_have_report_sender_in_context() {
        assertThat(context.getBeanNamesForType(EmailSender.class)).isEmpty();
    }
}
