package uk.gov.hmcts.reform.blobrouter.services.email;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.ApplicationContext;
import org.springframework.test.context.TestPropertySource;
import uk.gov.hmcts.reform.blobrouter.reconciliation.service.ReconciliationMailService;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@TestPropertySource(properties = "spring.mail.host=false")
public class EmailSenderDisabledTest {

    @Autowired
    private ApplicationContext context;

    @MockBean
    private ReconciliationMailService mailService;

    @Test
    void should_not_have_report_sender_in_context() {
        assertThat(context.getBeanNamesForType(EmailSender.class)).isEmpty();
        assertThat(context.getBeanNamesForType(MessageSender.class)).isNotEmpty();
    }
}
