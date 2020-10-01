package uk.gov.hmcts.reform.blobrouter.reconciliation.controller;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import uk.gov.hmcts.reform.blobrouter.config.TargetStorageAccount;
import uk.gov.hmcts.reform.blobrouter.reconciliation.service.DetailedReportService;
import uk.gov.hmcts.reform.blobrouter.reconciliation.service.ReconciliationMailService;
import uk.gov.hmcts.reform.blobrouter.reconciliation.service.SummaryReportService;

import java.time.LocalDate;
import java.util.Arrays;

import static org.springframework.format.annotation.DateTimeFormat.ISO.DATE;

@RestController
public class ReconciliationReportGenerationController {

    private final ReconciliationMailService reconciliationMailService;
    private final DetailedReportService detailedReportService;
    private final SummaryReportService summaryReportService;

    public ReconciliationReportGenerationController(
        ReconciliationMailService reconciliationMailService,
        DetailedReportService detailedReportService,
        SummaryReportService summaryReportService
    ) {
        this.reconciliationMailService = reconciliationMailService;
        this.detailedReportService = detailedReportService;
        this.summaryReportService = summaryReportService;
    }

    @PostMapping(path = "/reconciliation/generate-and-email-reports")
    public void generateAndEmailReports(
        @RequestParam(name = "date") @DateTimeFormat(iso = DATE) LocalDate date
    ) {
        // generate reports
        summaryReportService.process(date);
        detailedReportService.process(date, TargetStorageAccount.CFT);

        // email report
        reconciliationMailService.process(date, Arrays.asList(TargetStorageAccount.values()));
    }
}
