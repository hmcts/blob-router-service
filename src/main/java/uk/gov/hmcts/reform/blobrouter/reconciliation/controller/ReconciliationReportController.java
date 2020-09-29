package uk.gov.hmcts.reform.blobrouter.reconciliation.controller;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import uk.gov.hmcts.reform.blobrouter.config.TargetStorageAccount;
import uk.gov.hmcts.reform.blobrouter.model.out.SearchResult;
import uk.gov.hmcts.reform.blobrouter.reconciliation.service.DetailedReportService;
import uk.gov.hmcts.reform.blobrouter.reconciliation.service.ReconciliationMailService;
import uk.gov.hmcts.reform.blobrouter.reconciliation.service.ReconciliationService;
import uk.gov.hmcts.reform.blobrouter.reconciliation.service.SummaryReportService;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;

import static org.springframework.format.annotation.DateTimeFormat.ISO.DATE;

@RestController
@RequestMapping(path = "/reconciliation-reports", produces = MediaType.APPLICATION_JSON_VALUE)
public class ReconciliationReportController {

    private final ReconciliationService service;
    private final ReconciliationMailService reconciliationMailService;
    private final DetailedReportService detailedReportService;
    private final SummaryReportService summaryReportService;

    public ReconciliationReportController(
        ReconciliationService service,
        SummaryReportService summaryReportService,
        DetailedReportService detailedReportService,
        ReconciliationMailService reconciliationMailService
    ) {
        this.service = service;
        this.summaryReportService = summaryReportService;
        this.detailedReportService = detailedReportService;
        this.reconciliationMailService = reconciliationMailService;
    }

    @GetMapping
    public SearchResult getReportsByDate(
        @RequestParam(name = "date") @DateTimeFormat(iso = DATE) LocalDate date
    ) {
        return new SearchResult(service.getReconciliationReports(date));
    }

    @GetMapping(path = "/generate-and-email-report")
    public void generateAndEmailReconciliationReports(
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
