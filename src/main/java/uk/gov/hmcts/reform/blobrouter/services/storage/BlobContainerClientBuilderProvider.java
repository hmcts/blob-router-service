package uk.gov.hmcts.reform.blobrouter.services.storage;

import com.azure.core.http.HttpClient;
import com.azure.storage.blob.BlobContainerClientBuilder;
import com.azure.storage.common.policy.RequestRetryOptions;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class BlobContainerClientBuilderProvider {

    final HttpClient httpClient;
    final String bulkScanStorageUrl;
    final String pcqStorageUrl;

    public BlobContainerClientBuilderProvider(
        HttpClient httpClient,
        @Value("${storage.bulkscan.url}") String bulkScanStorageUrl,
        @Value("${storage.pcq.url}") String pcqStorageUrl
    ) {
        this.httpClient = httpClient;
        this.bulkScanStorageUrl = bulkScanStorageUrl;
        this.pcqStorageUrl = pcqStorageUrl;
    }

    public BlobContainerClientBuilder getBlobContainerClientBuilder() {
        return new BlobContainerClientBuilder()
            .httpClient(httpClient)
            .endpoint(bulkScanStorageUrl);
    }

    public BlobContainerClientBuilder getPcqBlobContainerClientBuilder() {
        return new BlobContainerClientBuilder()
            .retryOptions(new RequestRetryOptions(null, null, 180, null, null, null))
            .endpoint(pcqStorageUrl);
    }
}
