package uk.gov.hmcts.reform.blobrouter.reconciliation.controller;

import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;
import uk.gov.hmcts.reform.blobrouter.exceptions.InvalidApiKeyException;
import uk.gov.hmcts.reform.blobrouter.reconciliation.model.in.SupplierStatement;
import uk.gov.hmcts.reform.blobrouter.reconciliation.model.out.SuccessfulResponse;
import uk.gov.hmcts.reform.blobrouter.reconciliation.service.ReconciliationService;
import uk.gov.hmcts.reform.blobrouter.reconciliation.service.datetimechecker.DateRelevantForCurrentReconciliationChecker;

import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.util.UUID;
import javax.validation.ClockProvider;
import javax.validation.Valid;

import static org.springframework.http.HttpHeaders.AUTHORIZATION;

@RestController
public class ReconciliationController {

    private final ReconciliationService service;

    private final String apiKey;

    private final ClockProvider clockProvider;

    private final DateRelevantForCurrentReconciliationChecker dateRelevantForCurrentReconciliationChecker;

    public ReconciliationController(
        ReconciliationService service,
        @Value("${reconciliation.api-key}") String apiKey,
        ClockProvider clockProvider
    ) {
        this.service = service;
        this.apiKey = apiKey;
        this.clockProvider = clockProvider;
        this.dateRelevantForCurrentReconciliationChecker = (dateTime) -> dateTime.getHour() < 6;
    }

    @PostMapping(
        path = "/reconciliation-report/{date}",
        consumes = MediaType.APPLICATION_JSON_VALUE,
        produces = MediaType.APPLICATION_JSON_VALUE
    )
    @ApiOperation("Saves supplier statements report for given date")
    @ApiResponses({
        @ApiResponse(
            code = 200, response = SuccessfulResponse.class, message = "The report has been accepted"
        ),
        @ApiResponse(code = 400, message = "Request failed due to malformed syntax in either body or path parameter"),
        @ApiResponse(code = 401, message = "Invalid API Key")
    })
    public SuccessfulResponse uploadSupplierReport(
        @RequestHeader(value = AUTHORIZATION, required = false) String authHeader,
        @PathVariable("date") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
        @Valid @RequestBody SupplierStatement supplierStatement
    ) {
        validateAuthorization(authHeader);
        UUID uuid = service.saveSupplierStatement(date, supplierStatement);

        if (dateRelevantForCurrentReconciliationChecker.isTimeRelevant(ZonedDateTime.now(clockProvider.getClock()))) {
            return new SuccessfulResponse(uuid.toString());
        } else {
            return new SuccessfulResponse(uuid.toString(), "IRRELEVANT REPORT");
        }
    }

    private void validateAuthorization(String authorizationKey) {

        if (StringUtils.isEmpty(authorizationKey)) {
            throw new InvalidApiKeyException("API Key is missing");
        } else if (!authorizationKey.equals("Bearer " + apiKey)) {
            throw new InvalidApiKeyException("Invalid API Key");
        }

    }
}
