package uk.gov.hmcts.reform.blobrouter.clients.pcq;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import uk.gov.hmcts.reform.blobrouter.clients.response.SasTokenResponse;

@FeignClient(
    name = "pcq-backend-client",
    url = "${pcq-backend-api-url}"
)
public interface PcqClient {

    @GetMapping("/pcq/backend/token/bulkscan")
    SasTokenResponse getSasToken(@RequestHeader("ServiceAuthorization") String serviceAuthorisation);
}
