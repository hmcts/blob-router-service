package uk.gov.hmcts.reform.blobrouter.reconciliation.controller;

import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import uk.gov.hmcts.reform.blobrouter.reconciliation.model.in.SupplierStatementReport;
import uk.gov.hmcts.reform.blobrouter.reconciliation.model.out.SuccessfulResponse;
import uk.gov.hmcts.reform.blobrouter.reconciliation.service.ReconciliationService;

import java.time.LocalDate;
import java.util.UUID;
import javax.validation.Valid;

@ConditionalOnProperty("reconciliation.enabled")
@RestController
public class ReconciliationController {

    private final ReconciliationService service;

    public ReconciliationController(ReconciliationService service) {
        this.service = service;
    }

    @PostMapping(
        path = "/reform-scan/reconciliation-report/{date}",
        consumes = MediaType.APPLICATION_JSON_VALUE,
        produces = MediaType.APPLICATION_JSON_VALUE
    )
    @ApiOperation("Saves supplier statements report for given date")
    @ApiResponses({
        @ApiResponse(
            code = 200, response = SuccessfulResponse.class, message = "The report has been accepted"
        ),
        @ApiResponse(code = 400, message = "Request failed due to malformed syntax in either body or path parameter"),
        @ApiResponse(code = 401, message = "Invalid SSL certificate/Invalid subscription key") //TODO: authentication
    })
    public SuccessfulResponse uploadSupplierReport(
        @PathVariable("date") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
        @Valid @RequestBody SupplierStatementReport report
    ) {
        UUID uuid = service.saveSupplierStatement(date, report.supplierStatement);
        return new SuccessfulResponse(uuid.toString());
    }
}
