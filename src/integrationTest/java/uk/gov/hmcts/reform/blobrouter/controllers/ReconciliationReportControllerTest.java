package uk.gov.hmcts.reform.blobrouter.controllers;

import org.hamcrest.core.IsNull;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.servlet.MockMvc;
import uk.gov.hmcts.reform.blobrouter.data.reconciliation.reports.model.ReconciliationReport;
import uk.gov.hmcts.reform.blobrouter.reconciliation.controller.ReconciliationReportController;
import uk.gov.hmcts.reform.blobrouter.reconciliation.service.ReconciliationService;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.UUID;

import static org.hamcrest.Matchers.hasSize;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(SpringExtension.class)
@WebMvcTest(ReconciliationReportController.class)
public class ReconciliationReportControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ReconciliationService service;

    private static final String VERSION = "v1";

    @Test
    void should_return_list_of_reports_for_given_date_by_request_param() throws Exception {

        LocalDate date = LocalDate.now();

        String summaryReport = "{\n"
            + "  \"actual_count\": 120,\n"
            + "  \"reported_count\": 140,\n"
            + "  \"received_but_not_reported\": [\n"
            + "    {\n"
            + "    \"zip_file_name\": \"312.zip\",\n"
            + "    \"container\": \"sscs\"\n"
            + "    }\n"
            + "  ]"
            + "}";

        String detailedReport = "{\n"
            + "  \"discrepancies\": [\n"
            + "    {\n"
            + "      \"zip_file_name\": \"file.zip\",\n"
            + "      \"stated\": \"[payment_dcn_1, payment_dcn_2]\",\n"
            + "    }\n"
            + "  ]\n"
            + "}";

        var report1 = new ReconciliationReport(
            UUID.randomUUID(),
            UUID.randomUUID(),
            "ACCOUNT",
            summaryReport,
            detailedReport,
            VERSION,
            null,
            LocalDateTime.now()
        );

        var report2 = new ReconciliationReport(
            UUID.randomUUID(),
            UUID.randomUUID(),
            "CFT",
            "{}",
            null,
            VERSION,
            LocalDateTime.now(),
            LocalDateTime.now()
        );

        given(service.getReconciliationReports(date))
            .willReturn(Arrays.asList(report1, report2));

        mockMvc
            .perform(
                get("/reconciliation-reports")
                    .queryParam("date", date.format(DateTimeFormatter.ISO_DATE))
            )
            .andDo(print())
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.count").value(2))
            .andExpect(jsonPath("$.data", hasSize(2)))
            .andExpect(jsonPath("$.data.[0].id").value(report1.id.toString()))
            .andExpect(jsonPath("$.data.[0].supplier_statement_id")
                .value(report1.supplierStatementId.toString()))
            .andExpect(jsonPath("$.data.[0].account").value(report1.account))
            .andExpect(jsonPath("$.data.[0].summary_content.actual_count").value(120))
            .andExpect(jsonPath("$.data.[0].summary_content.reported_count").value(140))
            .andExpect(jsonPath("$.data.[0].summary_content.received_but_not_reported[0].zip_file_name")
                .value("312.zip"))
            .andExpect(jsonPath("$.data.[0].summary_content.received_but_not_reported[0].container")
                .value("sscs"))
            .andExpect(jsonPath("$.data.[0].detailed_content.discrepancies[0].zip_file_name")
                .value("file.zip"))
            .andExpect(jsonPath("$.data.[0].detailed_content.discrepancies[0].stated")
                .value("[payment_dcn_1, payment_dcn_2]"))
            .andExpect(jsonPath("$.data.[1].id").value(report2.id.toString()))
            .andExpect(jsonPath("$.data.[1].supplier_statement_id")
                .value(report2.supplierStatementId.toString()))
            .andExpect(jsonPath("$.data.[1].account").value(report2.account))
            .andExpect(jsonPath("$.data.[1].summary_content").isEmpty())
            .andExpect(jsonPath("$.data.[1].detailed_content").value(IsNull.nullValue()));


        verify(service).getReconciliationReports(date);

    }

    @Test
    public void should_return_400_for_invalid_date() throws Exception {
        mockMvc.perform(
            get("/reconciliation-reports").queryParam("date", "20200911") // invalid date
        )
            .andDo(print())
            .andExpect(status().isBadRequest());
    }
}
