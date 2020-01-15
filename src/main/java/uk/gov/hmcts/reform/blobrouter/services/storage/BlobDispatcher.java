package uk.gov.hmcts.reform.blobrouter.services.storage;

import com.azure.storage.blob.BlobContainerClient;
import com.azure.storage.blob.BlobServiceClient;
import org.slf4j.Logger;

import java.io.ByteArrayInputStream;

import static org.slf4j.LoggerFactory.getLogger;

public class BlobDispatcher {

    private static final Logger logger = getLogger(BlobDispatcher.class);

    private final BlobServiceClient storageClient;

    public BlobDispatcher(BlobServiceClient storageClient) {
        this.storageClient = storageClient;
    }

    public void dispatch(String blobName, byte[] blobContents, String destinationContainer) {
        logger.info("Uploading {} to {} container", blobName, destinationContainer);

        try {
            getContainerClient(destinationContainer)
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
    private BlobContainerClient getContainerClient(String destinationContainer) {
        return storageClient.getBlobContainerClient(destinationContainer);
    }
}
