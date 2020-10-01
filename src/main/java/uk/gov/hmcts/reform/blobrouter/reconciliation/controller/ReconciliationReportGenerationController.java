package uk.gov.hmcts.reform.blobrouter.reconciliation.controller;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import uk.gov.hmcts.reform.blobrouter.config.TargetStorageAccount;
import uk.gov.hmcts.reform.blobrouter.reconciliation.service.CftDetailedReportService;
import uk.gov.hmcts.reform.blobrouter.reconciliation.service.ReconciliationMailService;
import uk.gov.hmcts.reform.blobrouter.reconciliation.service.SummaryReportService;

import java.time.LocalDate;
import java.util.Arrays;

import static org.springframework.format.annotation.DateTimeFormat.ISO.DATE;

@RestController
@RequestMapping(path = "/reconciliation-reports")
public class ReconciliationReportGenerationController {

    private final ReconciliationMailService reconciliationMailService;
    private final CftDetailedReportService cftDetailedReportService;
    private final SummaryReportService summaryReportService;

    public ReconciliationReportGenerationController(
        ReconciliationMailService reconciliationMailService,
        CftDetailedReportService cftDetailedReportService,
        SummaryReportService summaryReportService
    ) {
        this.reconciliationMailService = reconciliationMailService;
        this.cftDetailedReportService = cftDetailedReportService;
        this.summaryReportService = summaryReportService;
    }

    @PostMapping(path = "/generate-and-email-report")
    public void generateAndEmailReports(
        @RequestParam(name = "date") @DateTimeFormat(iso = DATE) LocalDate date
    ) {
        // generate reports
        summaryReportService.process(date);
        cftDetailedReportService.process(date);

        // email report
        reconciliationMailService.process(date, Arrays.asList(TargetStorageAccount.values()));
    }
}
