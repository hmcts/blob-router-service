package uk.gov.hmcts.reform.blobrouter.clients.bulkscanprocessor;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.google.common.io.Resources;
import feign.FeignException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.contract.wiremock.AutoConfigureWireMock;
import org.springframework.test.context.ActiveProfiles;
import uk.gov.hmcts.reform.blobrouter.reconciliation.report.DiscrepancyItem;
import uk.gov.hmcts.reform.blobrouter.reconciliation.report.ReconciliationReportResponse;
import uk.gov.hmcts.reform.blobrouter.reconciliation.report.ReconciliationStatement;
import uk.gov.hmcts.reform.blobrouter.reconciliation.report.ReportedZipFile;

import java.io.IOException;
import java.time.LocalDate;
import java.util.List;

import static com.github.tomakehurst.wiremock.client.WireMock.badRequest;
import static com.github.tomakehurst.wiremock.client.WireMock.equalToJson;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.okJson;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.google.common.io.Resources.getResource;
import static java.nio.charset.StandardCharsets.UTF_8;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowableOfType;

@AutoConfigureWireMock(port = 0)
@ActiveProfiles("integration-test")
@SpringBootTest(properties = "bulk-scan-processor-url=http://localhost:${wiremock.server.port}")
public class BulkScanProcessorClientTest {

    @Autowired
    private BulkScanProcessorClient client;

    @Test
    public void should_return_sas_token_when_everything_is_ok() throws JsonProcessingException {

        var expected = new SasTokenResponse("187*2@(*&^%$£@12");

        // given
        stubFor(get("/token/sscs").willReturn(okJson("{\"sas_token\":\"187*2@(*&^%$£@12\"}")));

        // when
        SasTokenResponse sasResponse = client.getSasToken("sscs");

        // then
        assertThat(sasResponse).isEqualToComparingFieldByField(expected);
    }

    @Test
    public void should_throw_exception_when_requested_service_is_not_configured() throws Exception {
        // given
        stubFor(get("/token/notFoundService").willReturn(badRequest()));

        // when
        FeignException.BadRequest exception = catchThrowableOfType(
            () -> client.getSasToken("notFoundService"),
            FeignException.BadRequest.class
        );
        // then
        assertThat(exception.status()).isEqualTo(400);

    }

    @Test
    public void should_get_reconciliation_report_with_discrepancy() throws IOException {

        String requestBody = Resources.toString(
            getResource("reconciliation/statement-request-to-processor.json"),
            UTF_8
        );

        String responseBody = Resources.toString(
            getResource("reconciliation/statement-response-with-discrepancies.json"),
            UTF_8
        );

        // given
        stubFor(post("/reports/reconciliation")
            .withRequestBody(equalToJson(requestBody))
            .willReturn(okJson(responseBody))
        );

        ReconciliationStatement reconciliationStatement =
            new ReconciliationStatement(
                LocalDate.of(2020,8, 31),
                List.of(
                    new ReportedZipFile("file_name_1.zip",
                        "container1",
                        null,
                        List.of("scan_dcn_1", "scan_dcn_2"),
                        List.of("payment_dcn_1", "payment_dcn_2")
                    ),
                    new ReportedZipFile("file_name_2.zip",
                        "container2",
                        "rescan_for_file_1.zip",
                        List.of("scan_dcn_4"),
                        null
                    )
                )
            );

        // when
        ReconciliationReportResponse response = client.postReconciliationReport(reconciliationStatement);

        // then
        assertThat(response.items.size()).isEqualTo(1);
        DiscrepancyItem discrepancyItem = response.items.get(0);
        assertThat(discrepancyItem.actual).isEqualTo("[payment_dcn_1]");
        assertThat(discrepancyItem.container).isEqualTo("container1");
        assertThat(discrepancyItem.stated).isEqualTo("[payment_dcn_1, payment_dcn_2]");
        assertThat(discrepancyItem.type).isEqualTo("payment dcns mismatch");
        assertThat(discrepancyItem.zipFileName).isEqualTo("file_name_1.zip");

    }

    @Test
    public void should_handle_reconciliation_report_with_null_list() throws IOException {

        String requestBody = Resources.toString(
            getResource("reconciliation/statement-request-to-processor.json"),
            UTF_8
        );

        String responseBody =  "{ \"discrepancies\": null }";
        // given
        stubFor(post("/reports/reconciliation")
            .withRequestBody(equalToJson(requestBody))
            .willReturn(okJson(responseBody))
        );

        ReconciliationStatement reconciliationStatement =
            new ReconciliationStatement(
                LocalDate.of(2020,8, 31),
                List.of(
                    new ReportedZipFile("file_name_1.zip",
                        "container1",
                        null,
                        List.of("scan_dcn_1", "scan_dcn_2"),
                        List.of("payment_dcn_1", "payment_dcn_2")
                    ),
                    new ReportedZipFile("file_name_2.zip",
                        "container2",
                        "rescan_for_file_1.zip",
                        List.of("scan_dcn_4"),
                        null
                    )
                )
            );

        // when
        ReconciliationReportResponse response = client.postReconciliationReport(reconciliationStatement);

        // then
        assertThat(response.items).isNull();

    }

}
