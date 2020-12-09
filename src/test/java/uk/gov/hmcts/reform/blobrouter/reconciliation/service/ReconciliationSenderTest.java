package uk.gov.hmcts.reform.blobrouter.reconciliation.service;

import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import uk.gov.hmcts.reform.blobrouter.services.email.EmailSender;

class ReconciliationSenderTest {
    private ReconciliationSender reconciliationSender;

    @Mock
    private EmailSender emailSender;

    @Mock
    private ReconciliationCsvWriter reconciliationCsvWriter;

    private static final String mailFrom = "from@f.com";
    private static final String[] mailRecipients = {"r1@d.com"};

    @Test
    void sendReconciliationReport() {
        // given
        reconciliationSender = getReconciliationSender(false);

        // when


        // then
    }

    private ReconciliationSender getReconciliationSender(boolean skipEmptyReports) {
        return new ReconciliationSender(
            emailSender,
            reconciliationCsvWriter,
            mailFrom,
            mailRecipients
        );
    }
}
