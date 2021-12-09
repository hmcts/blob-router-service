package uk.gov.hmcts.reform.blobrouter.controllers;

import io.swagger.v3.oas.annotations.Operation;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import uk.gov.hmcts.reform.blobrouter.data.reports.EnvelopeCountSummaryReportListResponse;
import uk.gov.hmcts.reform.blobrouter.model.out.reports.EnvelopeCountSummaryReportItem;
import uk.gov.hmcts.reform.blobrouter.services.report.ReportService;

import java.time.LocalDate;
import java.util.List;

import static java.util.stream.Collectors.toList;
import static org.springframework.format.annotation.DateTimeFormat.ISO.DATE;

@RestController
@RequestMapping(path = "/reports")
public class ReportsController {
    private final ReportService reportService;

    public ReportsController(
        ReportService reportService
    ) {
        this.reportService = reportService;
    }

    @GetMapping(path = "/count-summary", produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(description = "Retrieves envelope count summary report")
    public EnvelopeCountSummaryReportListResponse getCountSummary(
        @RequestParam(name = "date") @DateTimeFormat(iso = DATE) LocalDate date
    ) {
        List<EnvelopeCountSummaryReportItem> result = this.reportService.getCountFor(date);
        return new EnvelopeCountSummaryReportListResponse(result
                                                              .stream()
                                                              .map(item -> new EnvelopeCountSummaryReportItem(
                                                                  item.received,
                                                                  item.rejected,
                                                                  item.container,
                                                                  item.date
                                                              ))
                                                              .collect(toList()));
    }

}
