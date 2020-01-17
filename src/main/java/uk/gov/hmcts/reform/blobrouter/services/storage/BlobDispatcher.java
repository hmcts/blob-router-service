package uk.gov.hmcts.reform.blobrouter.services.storage;

import com.azure.storage.blob.BlobContainerClient;
import org.slf4j.Logger;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.reform.blobrouter.config.TargetStorageAccount;

import java.io.ByteArrayInputStream;

import static org.slf4j.LoggerFactory.getLogger;

@Component
public class BlobDispatcher {

    private static final Logger logger = getLogger(BlobDispatcher.class);

    private final BlobServiceClientProvider blobServiceClientProvider;

    public BlobDispatcher(BlobServiceClientProvider blobServiceClientProvider) {
        this.blobServiceClientProvider = blobServiceClientProvider;
    }

    public void dispatch(
        String blobName,
        byte[] blobContents,
        String destinationContainer,
        TargetStorageAccount targetStorageAccount
    ) {
        logger.info("Uploading {} to {} container. Storage: {}", blobName, destinationContainer, targetStorageAccount);

        getContainerClient(targetStorageAccount, destinationContainer)
            .getBlobClient(blobName)
            .getBlockBlobClient()
            .upload(
                new ByteArrayInputStream(blobContents),
                blobContents.length
            );

        logger.info(
            "Finished uploading {} to {} container. Storage: {}",
            blobName,
            destinationContainer,
            targetStorageAccount
        );
    }

    // will use different storageClient depending on container
    private BlobContainerClient getContainerClient(
        TargetStorageAccount targetStorageAccount,
        String destinationContainer
    ) {
        return blobServiceClientProvider
            .get(targetStorageAccount, destinationContainer)
            .getBlobContainerClient(destinationContainer);
    }
}
