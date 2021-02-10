package uk.gov.hmcts.reform.blobrouter.services.storage;

import com.azure.storage.blob.BlobClient;
import org.slf4j.Logger;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.reform.blobrouter.config.TargetStorageAccount;

import static org.slf4j.LoggerFactory.getLogger;

@Component
public class BlobDispatcher {

    private static final Logger logger = getLogger(BlobDispatcher.class);

    private final BlobContainerClientProxy blobContainerClientProxy;

    public BlobDispatcher(BlobContainerClientProxy blobContainerClientProxy) {
        this.blobContainerClientProxy = blobContainerClientProxy;
    }

    public void dispatch(
        BlobClient sourceBlob,
        String destinationContainer,
        TargetStorageAccount targetStorageAccount
    ) {
        logger.info(
            "Uploading file. Blob name: {}. Container: {}. Storage: {}",
            sourceBlob.getBlobName(),
            destinationContainer,
            targetStorageAccount
        );

        blobContainerClientProxy.streamContentToDestination(sourceBlob, destinationContainer, targetStorageAccount);

        logger.info(
            "Finished uploading file. Blob name: {}. Container: {}. Storage: {}",
            sourceBlob.getBlobName(),
            destinationContainer,
            targetStorageAccount
        );
    }

    public void moveBlob(
        BlobClient blob,
        String destinationContainer,
        TargetStorageAccount targetStorageAccount
    ) {
        logger.info(
            "Move file from: {} to Container: {}. Storage: {}",
            blob.getBlobUrl(),
            destinationContainer,
            targetStorageAccount
        );

        blobContainerClientProxy.moveBlob(blob, destinationContainer, targetStorageAccount);

        logger.info(
            "Finished moving blob: {} to Container: {}. Storage: {}",
            blob.getBlobUrl(),
            destinationContainer,
            targetStorageAccount
        );
    }
}
