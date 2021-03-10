package uk.gov.hmcts.reform.blobrouter.controllers;

import io.swagger.annotations.ApiOperation;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import uk.gov.hmcts.reform.blobrouter.model.out.EnvelopeCountSummary;
import uk.gov.hmcts.reform.blobrouter.model.out.EnvelopeCountSummaryItem;
import uk.gov.hmcts.reform.blobrouter.model.out.reports.EnvelopeCountSummaryReportListResponse;
import uk.gov.hmcts.reform.blobrouter.services.report.ReportService;

import java.time.LocalDate;
import java.util.List;

import static java.util.stream.Collectors.toList;
import static org.springframework.format.annotation.DateTimeFormat.ISO.DATE;

@RestController
@CrossOrigin
@RequestMapping(path = "/reports")


public class ReportsController {

    private final ReportService reportService;
    // region constructor
    public ReportsController(
        ReportService reportService
    ) {
        this.reportService = reportService;
    }
    // endregion
    @GetMapping(path = "/count-summary", produces = MediaType.APPLICATION_JSON_VALUE)
    @ApiOperation("Retrieves envelope count summary report")
    public EnvelopeCountSummaryReportListResponse getCountSummary(
        @RequestParam(name = "date") @DateTimeFormat(iso = DATE) LocalDate date,
        @RequestParam(name = "include-test", defaultValue = "false", required = false) boolean includeTestContainer
    ) {
        List<EnvelopeCountSummary> result = this.reportService.getCountFor(date, includeTestContainer);
        return new EnvelopeCountSummaryReportListResponse(result
                                                              .stream()
                                                              .map(item -> new EnvelopeCountSummary(
                                                                  item.received,
                                                                  item.rejected,
                                                                  item.container,
                                                                  item.date
                                                              ))
                                                              .collect(toList()));
    }
}
