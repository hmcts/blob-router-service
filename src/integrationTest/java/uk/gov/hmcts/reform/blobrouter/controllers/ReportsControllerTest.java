package uk.gov.hmcts.reform.blobrouter.controllers;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;
import uk.gov.hmcts.reform.blobrouter.data.reports.EnvelopeCountSummaryReportListResponse;
import uk.gov.hmcts.reform.blobrouter.model.out.reports.EnvelopeCountSummaryReportItem;
import uk.gov.hmcts.reform.blobrouter.services.report.ReportService;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

import static java.util.stream.Collectors.toList;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(value = ReportsController.class, properties = {
    "service.storage-config[0].source-container=crime",
    "service.storage-config[1].source-container=pcq" })
public class ReportsControllerTest {

    private static final String CRIME_CONTAINER = "crime";
    private static final String PCQ_CONTAINER = "pcq";

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ReportService reportService;

    @Test
    public void should_return_result_generated_by_the_service() throws Exception {

        final EnvelopeCountSummaryReportItem countSummaryOne = new EnvelopeCountSummaryReportItem(
            321, 21, CRIME_CONTAINER, LocalDate.of(2021, 3, 28)
        );
        final EnvelopeCountSummaryReportItem countSummaryTwo = new EnvelopeCountSummaryReportItem(
            232, 19, PCQ_CONTAINER, LocalDate.of(2021, 3, 28)
        );
        List<EnvelopeCountSummaryReportItem> envelopeCountSummaryList = new ArrayList<>();
        envelopeCountSummaryList.add(countSummaryOne);
        envelopeCountSummaryList.add(countSummaryTwo);
        given(reportService.getCountFor(LocalDate.of(2021, 3, 28)))
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
            .perform(get("/reports/count-summary?date=2021-03-28"))
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
    public void should_return_400_if_date_is_invalid() throws Exception {
        final String invalidDate = "2021-15-12";

        mockMvc
            .perform(get("/reports/count-summary?date=" + invalidDate))
            .andExpect(status().isBadRequest());
    }

}
