package uk.gov.hmcts.reform.blobrouter.reconciliation.model.in;

import org.junit.jupiter.api.Test;
import uk.gov.hmcts.reform.blobrouter.reconciliation.model.out.SuccessfulReconciliationResponse;

import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;

// TODO: Delete. Added for test coverage
class ReconciliationModelTest {

    @Test
    void should_create_reconciliation_request() {
        ReconciliationRequest request = new ReconciliationRequest(
            new Report(
                singletonList(new Envelope(
                    "2010404021234_02-06-2020-03-06-18.zip",
                    "2010404020001_01-06-2020-03-00-00.zip",
                    "bulkscan",
                    "BULKSCAN",
                    asList("2015404021234", "2015404022345"),
                    singletonList("1234512345")
                ))
            )
        );

        assertThat(request).isNotNull();
        assertThat(request.report).isNotNull();
        assertThat(request.report.envelopes).isNotEmpty();
    }

    @Test
    void should_create_reconciliation_response() {
        SuccessfulReconciliationResponse response = new SuccessfulReconciliationResponse(
            "11186dcd-6f75-4e63-8c13-371eedd8b2b6"
        );

        assertThat(response).isNotNull();
        assertThat(response.id).isEqualTo("11186dcd-6f75-4e63-8c13-371eedd8b2b6");
    }

}
