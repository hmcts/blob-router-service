package uk.gov.hmcts.reform.blobrouter.reconciliation.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;
import uk.gov.hmcts.reform.blobrouter.reconciliation.model.in.SupplierStatement;
import uk.gov.hmcts.reform.blobrouter.reconciliation.model.out.SuccessfulResponse;
import uk.gov.hmcts.reform.blobrouter.reconciliation.service.ReconciliationService;
import uk.gov.hmcts.reform.blobrouter.reconciliation.service.datetimechecker.StatementRelevancyForAutomatedReportChecker;

import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.util.UUID;
import javax.validation.ClockProvider;
import javax.validation.Valid;

import static java.lang.String.format;
import static org.slf4j.LoggerFactory.getLogger;
import static org.springframework.http.HttpHeaders.AUTHORIZATION;
import static uk.gov.hmcts.reform.blobrouter.util.AuthValidator.validateAuthorization;

@RestController
public class ReconciliationController {
    private static final Logger logger = getLogger(ReconciliationController.class);

    private final ReconciliationService service;

    private final String apiKey;

    private final ClockProvider clockProvider;

    private final StatementRelevancyForAutomatedReportChecker statementRelevancyChecker;

    public ReconciliationController(
        ReconciliationService service,
        @Value("${reconciliation.api-key}") String apiKey,
        ClockProvider clockProvider,
        StatementRelevancyForAutomatedReportChecker statementRelevancyChecker
    ) {
        this.service = service;
        this.apiKey = apiKey;
        this.clockProvider = clockProvider;
        this.statementRelevancyChecker = statementRelevancyChecker;
    }

    @PostMapping(
        path = "/reconciliation-report/{date}",
        consumes = MediaType.APPLICATION_JSON_VALUE,
        produces = MediaType.APPLICATION_JSON_VALUE
    )
    @Operation(description = "Saves supplier statements report for given date")
    @ApiResponses({
        @ApiResponse(
            responseCode = "200",
            content = @Content(schema = @Schema(implementation = SuccessfulResponse.class)),
            description = "The report has been accepted"
        ),
        @ApiResponse(responseCode = "400",
            description = "Request failed due to malformed syntax in either body or path parameter"),
        @ApiResponse(responseCode = "401", description = "Invalid API Key")
    })
    public SuccessfulResponse uploadSupplierReport(
        @RequestHeader(value = AUTHORIZATION, required = false) String authHeader,
        @PathVariable("date") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
        @Valid @RequestBody SupplierStatement supplierStatement
    ) {
        logger.info("Supplier statement received for {}", date);
        validateAuthorization(authHeader, apiKey);
        UUID uuid = service.saveSupplierStatement(date, supplierStatement);

        ZonedDateTime now = ZonedDateTime.now(clockProvider.getClock());
        if (statementRelevancyChecker.isTimeRelevant(now, date)) {
            logger.info("Supplier statement saved with Id: {}", uuid);
            return new SuccessfulResponse(uuid.toString());
        } else {
            logger.warn("Submitted statement with ID: {} for date {} was submitted after the report was generated",
                        uuid, date);
            return new SuccessfulResponse(
                uuid.toString(),
                format(
                    "Provided statement is not going to be used for generating report for the date: %s. "
                        + "The report was already generated. In order to include this statement in the report"
                        + "it needs to be generated manually.", date
                )
            );
        }
    }
}
