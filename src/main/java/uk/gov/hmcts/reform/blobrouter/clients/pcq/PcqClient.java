package uk.gov.hmcts.reform.blobrouter.clients.pcq;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import uk.gov.hmcts.reform.blobrouter.clients.response.SasTokenResponse;

import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

@FeignClient(
    name = "pcq-backend-client",
    url = "${pcq-backend-api-url}"
)
public interface PcqClient {

    @GetMapping(value = "/pcq/backend/token/bulkscan", consumes = APPLICATION_JSON_VALUE)
    SasTokenResponse getSasToken(@RequestHeader("ServiceAuthorization") String serviceAuthorisation);
}
