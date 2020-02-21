package uk.gov.hmcts.reform.blobrouter.services.storage;

import static org.slf4j.LoggerFactory.getLogger;

import com.azure.storage.blob.BlobContainerClient;
import com.azure.storage.blob.BlobContainerClientBuilder;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.blobrouter.clients.bulkscanprocessor.BulkScanProcessorClient;
import uk.gov.hmcts.reform.blobrouter.config.TargetStorageAccount;

@Service
public class BlobContainerClientProvider {

    private static final Logger logger = getLogger(BlobContainerClientProvider.class);

    private final BlobContainerClient crimeClient;
    private final BulkScanProcessorClient bulkScanSasTokenClient;
    private final String bulkScanStorageUrl;

    public BlobContainerClientProvider(
        @Qualifier("crime-storage-client") BlobContainerClient crimeClient,
        BulkScanProcessorClient bulkScanSasTokenClient,
        @Value("${storage.bulkscan.url}") String bulkScanStorageUrl
    ) {
        this.crimeClient = crimeClient;
        this.bulkScanSasTokenClient = bulkScanSasTokenClient;
        this.bulkScanStorageUrl = bulkScanStorageUrl;
    }

    public BlobContainerClient get(TargetStorageAccount targetStorageAccount, String containerName) {
        switch (targetStorageAccount) {
            case BULKSCAN:
                // retrieving a SAS token every time we're getting a client, but this will be cached in the future
                String sasToken = bulkScanSasTokenClient.getSasToken(containerName).sasToken;
                logger.info("sasToken {}", sasToken);

                String connectionString = String.format(
                    "DefaultEndpointsProtocol=https;BlobEndpoint=%s;SharedAccessSignature=%s",
                    bulkScanStorageUrl,
                    sasToken
                );

                BlobContainerClient blobContainerClient = new BlobContainerClientBuilder()
                    .connectionString(connectionString)
                    .containerName(containerName)
                    .buildClient();

                logger.info("getBlobContainerUrl {}", blobContainerClient.getBlobContainerUrl());
                return blobContainerClient;
            case CRIME:
                return crimeClient;
            default:
                throw new UnknownStorageAccountException(
                    String.format("Client requested for an unknown storage account: %s", targetStorageAccount)
                );
        }
    }
}
