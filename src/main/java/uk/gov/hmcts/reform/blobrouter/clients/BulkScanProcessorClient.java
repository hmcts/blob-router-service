package uk.gov.hmcts.reform.blobrouter.clients;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

@FeignClient(
    name = "bulk-scan-processor",
    url = "${bulk-scan-processor-url}"
)
public interface BulkScanProcessorClient {

    @GetMapping(value = "/token/{container}",
        consumes = APPLICATION_JSON_VALUE,
        produces = APPLICATION_JSON_VALUE
    )
    SasTokenResponse getSasToken(@PathVariable String container);
}
