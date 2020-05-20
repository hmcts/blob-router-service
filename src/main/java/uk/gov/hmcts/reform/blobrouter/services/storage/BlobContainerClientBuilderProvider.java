package uk.gov.hmcts.reform.blobrouter.services.storage;

import com.azure.core.http.HttpClient;
import com.azure.storage.blob.BlobContainerClientBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class BlobContainerClientBuilderProvider {

    final HttpClient httpClient;
    final String bulkScanStorageUrl;

    public BlobContainerClientBuilderProvider(
        HttpClient httpClient,
        @Value("${storage.bulkscan.url:}") String bulkScanStorageUrl
    ) {
        this.httpClient = httpClient;
        this.bulkScanStorageUrl = bulkScanStorageUrl;
    }

    public BlobContainerClientBuilder getBlobContainerClientBuilder() {
        return new BlobContainerClientBuilder()
            .httpClient(httpClient)
            .endpoint(bulkScanStorageUrl);
    }
}
