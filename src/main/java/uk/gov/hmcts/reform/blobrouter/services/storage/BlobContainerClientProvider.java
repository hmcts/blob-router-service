package uk.gov.hmcts.reform.blobrouter.services.storage;

import com.azure.storage.blob.BlobContainerClient;
import com.azure.storage.blob.BlobContainerClientBuilder;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.blobrouter.config.TargetStorageAccount;

@Service
public class BlobContainerClientProvider {

    private final BlobContainerClient crimeClient;
    private final BlobContainerClientBuilder blobContainerClientBuilder;
    private final BulkScanSasTokenCache bulkScanSasTokenCache;

    public BlobContainerClientProvider(
        @Qualifier("crime-storage-client") BlobContainerClient crimeClient,
        @Qualifier("bulk-scan-blob-client-builder") BlobContainerClientBuilder blobContainerClientBuilder,
        BulkScanSasTokenCache bulkScanSasTokenCache
    ) {
        this.crimeClient = crimeClient;
        this.blobContainerClientBuilder = blobContainerClientBuilder;
        this.bulkScanSasTokenCache = bulkScanSasTokenCache;
    }

    public BlobContainerClient get(TargetStorageAccount targetStorageAccount, String containerName) {
        switch (targetStorageAccount) {
            case BULKSCAN:
                return blobContainerClientBuilder
                    .sasToken(bulkScanSasTokenCache.getSasToken(containerName))
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
