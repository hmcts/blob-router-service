package uk.gov.hmcts.reform.blobrouter.clients.bulkscanprocessor;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

@Component
public class BulkScanProcessorClient {

    private static final Logger log = LoggerFactory.getLogger(BulkScanProcessorClient.class);

    private final String baseUrl;
    private final RestTemplate restTemplate;

    public BulkScanProcessorClient(
        @Value("${bulk-scan-processor-url}")String baseUrl,
        RestTemplate restTemplate
    ) {
        this.baseUrl = baseUrl;
        this.restTemplate = restTemplate;
    }

    public SasTokenResponse getSasToken(String service) {
        String url =
            UriComponentsBuilder
                .fromHttpUrl(baseUrl)
                .path("/token/" + service)
                .build()
                .toString();

        log.info("Requesting SAS token, URL: {}", url);

        SasTokenResponse response = restTemplate.getForObject(
            url,
            SasTokenResponse.class
        );

        log.info("SAS token received for service {}", service);

        return response;
    }
}
