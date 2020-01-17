package uk.gov.hmcts.reform.blobrouter.services.storage;

import com.azure.storage.blob.BlobContainerClient;
import com.azure.storage.blob.BlobServiceClient;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Qualifier;
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
        logger.info("Uploading {} to {} container", blobName, destinationContainer);

        try {
            getContainerClient(targetStorageAccount, destinationContainer)
                .getBlobClient(blobName)
                .getBlockBlobClient()
                .upload(
                    new ByteArrayInputStream(blobContents),
                    blobContents.length
                );

            logger.info("Finished uploading {} to {} container", blobName, destinationContainer);
        } catch (Exception exception) {
            logger.error(
                "Error occurred while uploading {} to {} container",
                blobName,
                destinationContainer,
                exception
            );
        }
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
