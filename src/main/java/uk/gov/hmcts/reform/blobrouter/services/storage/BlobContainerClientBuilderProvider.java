package uk.gov.hmcts.reform.blobrouter.services.storage;

import com.azure.core.http.HttpClient;
import com.azure.storage.blob.BlobContainerClientBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * The `BlobContainerClientBuilderProvider` class in Java provides methods to create BlobContainerClientBuilder objects
 * with specified HTTP client and endpoint URLs for bulk scan and PCQ storage.
 */
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

    /**
     * The function returns a BlobContainerClientBuilder with specified HTTP client and endpoint.
     *
     * @return A BlobContainerClientBuilder object is being returned.
     */
    public BlobContainerClientBuilder getBlobContainerClientBuilder() {
        return new BlobContainerClientBuilder()
            .httpClient(httpClient)
            .endpoint(bulkScanStorageUrl);
    }

    /**
     * The function returns a BlobContainerClientBuilder configured with an HTTP client and a specified endpoint URL.
     *
     * @return A `BlobContainerClientBuilder` object is being returned.
     */
    public BlobContainerClientBuilder getPcqBlobContainerClientBuilder() {
        return new BlobContainerClientBuilder()
            .httpClient(httpClient)
            .endpoint(pcqStorageUrl);
    }
}
