package uk.gov.hmcts.reform.blobrouter.services.storage;

import com.azure.storage.blob.BlobContainerClient;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.blobrouter.config.TargetStorageAccount;

@Service
public class BlobContainerClientProvider {

    private final BlobContainerClient crimeClient;
    private final BlobContainerClientBuilderProvider blobContainerClientBuilderProvider;
    private final BulkScanSasTokenCache bulkScanSasTokenCache;

    public BlobContainerClientProvider(
        @Qualifier("crime-storage-client") BlobContainerClient crimeClient,
        BlobContainerClientBuilderProvider blobContainerClientBuilderProvider,
        BulkScanSasTokenCache bulkScanSasTokenCache
    ) {
        this.crimeClient = crimeClient;
        this.blobContainerClientBuilderProvider = blobContainerClientBuilderProvider;
        this.bulkScanSasTokenCache = bulkScanSasTokenCache;
    }

    public BlobContainerClient get(TargetStorageAccount targetStorageAccount, String containerName) {
        switch (targetStorageAccount) {
            case BULKSCAN:
                return blobContainerClientBuilderProvider
                    .getBlobContainerClientBuilderBean()
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
