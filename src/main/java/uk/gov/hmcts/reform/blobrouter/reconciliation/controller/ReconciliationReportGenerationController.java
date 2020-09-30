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
import java.util.List;

import static org.springframework.format.annotation.DateTimeFormat.ISO.DATE;

@RestController
@RequestMapping(path = "/reconciliation-reports")
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

    @PostMapping(path = "/generate-and-email-report")
    public void generateAndEmailReports(
        @RequestParam(name = "date") @DateTimeFormat(iso = DATE) LocalDate date
    ) {
        List<TargetStorageAccount> availableAccounts = Arrays.asList(
            TargetStorageAccount.CFT, TargetStorageAccount.CRIME
        );

        // generate reports
        summaryReportService.process(date);
        detailedReportService.process(date, TargetStorageAccount.CFT);

        // email report
        reconciliationMailService.process(date, availableAccounts);
    }
}
