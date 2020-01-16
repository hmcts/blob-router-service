package uk.gov.hmcts.reform.blobrouter.clients;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

@Component
public class BulkScanProcessorClient {

    private final RestTemplate restTemplate;

    private final String bulkScanProcessorUrl;

    public BulkScanProcessorClient(
        RestTemplate restTemplate,
        @Value("bulk-scan-processor-url") String bulkScanProcessorUrl
    ) {
        System.out.println("bulkScanProcessorUrl " + bulkScanProcessorUrl);
        this.restTemplate = restTemplate;
        this.bulkScanProcessorUrl = bulkScanProcessorUrl;
    }


    public SasTokenResponse getSasToken(String container) {

        String url =
            UriComponentsBuilder
                .fromHttpUrl(bulkScanProcessorUrl)
                .path("/token")
                .path(container)
                .build()
                .toString();

        return restTemplate.getForObject(url, SasTokenResponse.class);
    }
}
