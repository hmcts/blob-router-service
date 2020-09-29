package uk.gov.hmcts.reform.blobrouter.reconciliation.controller;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import uk.gov.hmcts.reform.blobrouter.model.out.SearchResult;
import uk.gov.hmcts.reform.blobrouter.reconciliation.service.ReconciliationService;

import java.time.LocalDate;

import static org.springframework.format.annotation.DateTimeFormat.ISO.DATE;

@RestController
@RequestMapping(path = "/reconciliation-reports", produces = MediaType.APPLICATION_JSON_VALUE)
public class ReconciliationReportController {

    private final ReconciliationService service;

    public ReconciliationReportController(ReconciliationService service) {
        this.service = service;
    }

    @GetMapping
    public SearchResult getReportsByDate(
        @RequestParam(name = "date") @DateTimeFormat(iso = DATE) LocalDate date
    ) {
        return new SearchResult(service.getReconciliationReports(date));
    }
}
