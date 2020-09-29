package uk.gov.hmcts.reform.blobrouter.controllers;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.servlet.MockMvc;
import uk.gov.hmcts.reform.blobrouter.reconciliation.controller.ReconciliationReportGenerationController;
import uk.gov.hmcts.reform.blobrouter.reconciliation.service.DetailedReportService;
import uk.gov.hmcts.reform.blobrouter.reconciliation.service.ReconciliationMailService;
import uk.gov.hmcts.reform.blobrouter.reconciliation.service.SummaryReportService;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(SpringExtension.class)
@WebMvcTest(
    value = ReconciliationReportGenerationController.class,
    properties = "reconciliation.manual-report-generation-and-email-enabled=false"
)
public class ReconciliationReportGenerationDisabledTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private SummaryReportService summaryReportService;

    @MockBean
    private DetailedReportService detailedReportService;

    @MockBean
    private ReconciliationMailService mailService;

    @Test
    public void should_return_404_when_email_config_is_disabled() throws Exception {
        // given
        LocalDate date = LocalDate.of(2020, 9, 10);

        // when
        // then
        mockMvc.perform(
            post("/reconciliation-reports/generate-and-email-report")
                .queryParam("date", date.format(DateTimeFormatter.ISO_DATE))
        )
            .andDo(print())
            .andExpect(status().isNotFound());

        verifyNoInteractions(summaryReportService);
        verifyNoInteractions(detailedReportService);
        verifyNoInteractions(mailService);
    }
}
