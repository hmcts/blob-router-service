package uk.gov.hmcts.reform.blobrouter.reconciliation.controller;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import uk.gov.hmcts.reform.blobrouter.config.TargetStorageAccount;
import uk.gov.hmcts.reform.blobrouter.exceptions.InvalidApiKeyException;
import uk.gov.hmcts.reform.blobrouter.reconciliation.service.CftDetailedReportService;
import uk.gov.hmcts.reform.blobrouter.reconciliation.service.ReconciliationMailService;
import uk.gov.hmcts.reform.blobrouter.reconciliation.service.SummaryReportService;

import java.time.LocalDate;
import java.util.Arrays;

import static org.slf4j.LoggerFactory.getLogger;
import static org.springframework.format.annotation.DateTimeFormat.ISO.DATE;
import static org.springframework.http.HttpHeaders.AUTHORIZATION;

@RestController
public class ReconciliationReportGenerationController {
    private static final Logger logger = getLogger(ReconciliationReportController.class);

    private final ReconciliationMailService reconciliationMailService;
    private final CftDetailedReportService cftDetailedReportService;
    private final SummaryReportService summaryReportService;
    private final String reportApiKey;

    public ReconciliationReportGenerationController(
        ReconciliationMailService reconciliationMailService,
        CftDetailedReportService cftDetailedReportService,
        SummaryReportService summaryReportService,
        @Value("${reconciliation.report.api-key}") String apiKey
    ) {
        this.reconciliationMailService = reconciliationMailService;
        this.cftDetailedReportService = cftDetailedReportService;
        this.summaryReportService = summaryReportService;
        this.reportApiKey = apiKey;
    }

    @PostMapping(path = "/reconciliation/generate-and-email-reports")
    public void generateAndEmailReports(
        @RequestHeader(value = AUTHORIZATION, required = false) String authHeader,
        @RequestParam(name = "date") @DateTimeFormat(iso = DATE) LocalDate date
    ) {
        validateAuthorization(authHeader);

        // generate reports
        summaryReportService.process(date);
        cftDetailedReportService.process(date);

        // email report
        reconciliationMailService.process(date, Arrays.asList(TargetStorageAccount.values()));
    }

    private void validateAuthorization(String authorizationKey) {

        if (StringUtils.isEmpty(authorizationKey)) {
            logger.error("API Key is missing");
            throw new InvalidApiKeyException("API Key is missing");
        } else if (!authorizationKey.equals("Bearer " + reportApiKey)) {
            logger.error("Invalid API Key");
            throw new InvalidApiKeyException("Invalid API Key");
        }

    }
}
