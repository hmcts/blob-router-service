package uk.gov.hmcts.reform.blobrouter.controller;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;
import uk.gov.hmcts.reform.blobrouter.controllers.ReportsController;
import uk.gov.hmcts.reform.blobrouter.model.out.reports.EnvelopeCountSummaryReportItem;
import uk.gov.hmcts.reform.blobrouter.model.out.reports.EnvelopeCountSummaryReportListResponse;
import uk.gov.hmcts.reform.blobrouter.services.report.ReportService;
import uk.gov.hmcts.reform.blobrouter.services.report.models.EnvelopeCountSummary;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

import static java.util.stream.Collectors.toList;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ReportsController.class)
public class ReportsControllerTest {

    @MockBean
    private ReportService reportService;

    @Autowired
    private MockMvc mockMvc;

    @Test
    public void should_return_result_generated_by_the_service() throws Exception {

        final EnvelopeCountSummary countSummaryOne = new EnvelopeCountSummary(
            152, 11, "container1", LocalDate.of(2021, 3, 4)
        );
        final EnvelopeCountSummary countSummaryTwo = new EnvelopeCountSummary(
            178, 13, "container2", LocalDate.of(2021, 3, 4)
        );
        List<EnvelopeCountSummary> envelopeCountSummaryList = new ArrayList<>();
        envelopeCountSummaryList.add(countSummaryOne);
        envelopeCountSummaryList.add(countSummaryTwo);
        given(reportService.getCountFor(LocalDate.of(2021, 3, 4), false))
            .willReturn(envelopeCountSummaryList);

        EnvelopeCountSummaryReportListResponse response = new EnvelopeCountSummaryReportListResponse(
            envelopeCountSummaryList.stream()
                .map(item -> new EnvelopeCountSummaryReportItem(
                    item.received,
                    item.rejected,
                    item.container,
                    item.date
                ))
                .collect(toList())
        );

        mockMvc
            .perform(get("/reports/count-summary?date=2021-03-04"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.total_received").value(response.totalReceived))
            .andExpect(jsonPath("$.total_rejected").value(response.totalRejected))
            .andExpect(jsonPath("$.time_stamp").value(response.timeStamp.format(
                DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))))
            .andExpect(jsonPath("$.data.length()").value(2))
            .andExpect(jsonPath("$.data[0].received").value(response.items.get(0).received))
            .andExpect(jsonPath("$.data[0].rejected").value(response.items.get(0).rejected))
            .andExpect(jsonPath("$.data[0].container").value(response.items.get(0).container))
            .andExpect(jsonPath("$.data[0].date").value(response.items.get(0).date.toString()));
    }

    @Test
    public void should_not_include_test_container_by_default() throws Exception {
        mockMvc.perform(get("/reports/count-summary?date=2019-01-14"));
        verify(reportService).getCountFor(LocalDate.of(2019, 1, 14), false);
    }

    @Test
    public void should_include_test_container_if_requested_by_the_client() throws Exception {
        mockMvc.perform(get("/reports/count-summary?date=2019-01-14&include-test=true"));

        verify(reportService).getCountFor(LocalDate.of(2019, 1, 14), true);
    }

    @Test
    public void should_not_include_test_container_if_exlicitly_not_requested_by_the_client() throws Exception {
        mockMvc.perform(get("/reports/count-summary?date=2019-01-14&include-test=false"));

        verify(reportService).getCountFor(LocalDate.of(2019, 1, 14), false);
    }

    @Test
    public void should_return_400_if_date_is_invalid() throws Exception {
        final String invalidDate = "2019-14-14";

        mockMvc
            .perform(get("/reports/count-summary?date=" + invalidDate))
            .andExpect(status().isBadRequest());
    }
}
