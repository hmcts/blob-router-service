package uk.gov.hmcts.reform.blobrouter.services.storage;

import com.azure.core.exception.HttpResponseException;
import com.azure.storage.blob.BlobContainerClient;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.blobrouter.config.TargetStorageAccount;

import java.io.ByteArrayInputStream;

@Service
public class BlobContainerClientProxy {

    //private final BlobContainerClient crimeClient;
    private final BlobContainerClientBuilderProvider blobContainerClientBuilderProvider;
    private final BulkScanSasTokenCache bulkScanSasTokenCache;

    public BlobContainerClientProxy(
        //@Qualifier("crime-storage-client") BlobContainerClient crimeClient,
        BlobContainerClientBuilderProvider blobContainerClientBuilderProvider,
        BulkScanSasTokenCache bulkScanSasTokenCache
    ) {
        //this.crimeClient = crimeClient;
        this.blobContainerClientBuilderProvider = blobContainerClientBuilderProvider;
        this.bulkScanSasTokenCache = bulkScanSasTokenCache;
    }

    private BlobContainerClient get(TargetStorageAccount targetStorageAccount, String containerName) {
        switch (targetStorageAccount) {
            case BULKSCAN:
                return blobContainerClientBuilderProvider
                    .getBlobContainerClientBuilder()
                    .sasToken(bulkScanSasTokenCache.getSasToken(containerName))
                    .containerName(containerName)
                    .buildClient();
//            case CRIME:
//                return crimeClient;
            default:
                throw new UnknownStorageAccountException(
                    String.format("Client requested for an unknown storage account: %s", targetStorageAccount)
                );
        }
    }

    public void upload(
        String blobName,
        byte[] blobContents,
        String destinationContainer,
        TargetStorageAccount targetStorageAccount
    ) {
        try {
            get(targetStorageAccount, destinationContainer)
                .getBlobClient(blobName)
                .getBlockBlobClient()
                .upload(
                    new ByteArrayInputStream(blobContents),
                    blobContents.length
                );
        } catch (HttpResponseException ex) {
            if (targetStorageAccount == TargetStorageAccount.BULKSCAN
                && HttpStatus.valueOf(ex.getResponse().getStatusCode()).is4xxClientError()) {
                bulkScanSasTokenCache.removeFromCache(destinationContainer);
            }
            throw ex;
        }
    }
}
