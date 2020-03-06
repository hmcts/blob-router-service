package uk.gov.hmcts.reform.blobrouter.services.storage;

import com.azure.core.http.HttpClient;
import com.azure.storage.blob.BlobContainerClient;
import com.azure.storage.blob.BlobContainerClientBuilder;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.blobrouter.config.TargetStorageAccount;

@Service
public class BlobContainerClientProvider {

    private final BlobContainerClient crimeClient;
    private final String bulkScanStorageUrl;
    private final HttpClient httpClient;
    private final BulkScanContainerClientCache bulkScanContainerClientCache;

    public BlobContainerClientProvider(
        @Qualifier("crime-storage-client") BlobContainerClient crimeClient,
        @Value("${storage.bulkscan.url}") String bulkScanStorageUrl,
        HttpClient httpClient,
        BulkScanContainerClientCache bulkScanContainerClientCache
    ) {
        this.crimeClient = crimeClient;
        this.bulkScanStorageUrl = bulkScanStorageUrl;
        this.httpClient = httpClient;
        this.bulkScanContainerClientCache = bulkScanContainerClientCache;
    }

    public BlobContainerClient get(TargetStorageAccount targetStorageAccount, String containerName) {
        switch (targetStorageAccount) {
            case BULKSCAN:
                // retrieving a SAS token every time we're getting a client, but this will be cached in the future
                return new BlobContainerClientBuilder()
                    .httpClient(httpClient)
                    .sasToken(bulkScanContainerClientCache.getSasToken(containerName))
                    .endpoint(bulkScanStorageUrl)
                    .containerName(containerName)
                    .buildClient();
            case CRIME:
                return crimeClient;
            default:
                throw new UnknownStorageAccountException(
                    String.format("Client requested for an unknown storage account: %s", targetStorageAccount)
                );
        }
    }
}
