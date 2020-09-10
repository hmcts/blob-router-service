package uk.gov.hmcts.reform.blobrouter.clients.bulkscanprocessor;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import uk.gov.hmcts.reform.blobrouter.clients.response.SasTokenResponse;
import uk.gov.hmcts.reform.blobrouter.reconciliation.report.ReconciliationReportResponse;
import uk.gov.hmcts.reform.blobrouter.reconciliation.report.ReconciliationStatement;

import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

@FeignClient(
    name = "bulk-scan-processor-client",
    url = "${bulk-scan-processor-url}"
)
public interface BulkScanProcessorClient {

    @GetMapping(value = "/token/{service}", consumes = APPLICATION_JSON_VALUE)
    SasTokenResponse getSasToken(@PathVariable("service") String service);

    @PostMapping(value = "/reports/reconciliation",
        consumes = APPLICATION_JSON_VALUE,
        produces = APPLICATION_JSON_VALUE
    )
    ReconciliationReportResponse postReconciliationReport(@RequestBody ReconciliationStatement statement);
}
