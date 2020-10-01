package uk.gov.hmcts.reform.blobrouter.controllers;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.servlet.MockMvc;
import uk.gov.hmcts.reform.blobrouter.config.TargetStorageAccount;
import uk.gov.hmcts.reform.blobrouter.reconciliation.controller.ReconciliationReportGenerationController;
import uk.gov.hmcts.reform.blobrouter.reconciliation.service.CftDetailedReportService;
import uk.gov.hmcts.reform.blobrouter.reconciliation.service.ReconciliationMailService;
import uk.gov.hmcts.reform.blobrouter.reconciliation.service.SummaryReportService;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;

import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(SpringExtension.class)
@WebMvcTest(ReconciliationReportGenerationController.class)
public class ReconciliationReportGenerationControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private SummaryReportService summaryReportService;

    @MockBean
    private CftDetailedReportService detailedReportService;

    @MockBean
    private ReconciliationMailService mailService;

    @Test
    public void should_return_400_when_the_given_date_is_invalid() throws Exception {
        mockMvc.perform(
            post("/reconciliation-reports/generate-and-email-report")
                .queryParam("date", "20200911") // invalid date
        )
            .andDo(print())
            .andExpect(status().isBadRequest());
    }

    @Test
    void should_generate_reconciliation_reports_and_send_email_when_the_given_date_is_valid() throws Exception {
        // given
        LocalDate date = LocalDate.of(2020, 9, 10);

        // when
        // then
        mockMvc
            .perform(
                post("/reconciliation-reports/generate-and-email-report")
                    .queryParam("date", date.format(DateTimeFormatter.ISO_DATE))
            )
            .andDo(print())
            .andExpect(status().isOk());

        verify(summaryReportService).process(date);
        verify(detailedReportService).process(date);
        verify(mailService).process(date, Arrays.asList(TargetStorageAccount.values()));
    }
}
