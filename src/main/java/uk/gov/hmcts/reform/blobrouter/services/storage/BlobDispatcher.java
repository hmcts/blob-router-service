package uk.gov.hmcts.reform.blobrouter.services.storage;

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
        String blobName,
        byte[] blobContents,
        String destinationContainer,
        TargetStorageAccount targetStorageAccount
    ) {
        logger.info(
            "Uploading file. Blob name: {}. Container: {}. Storage: {}",
            blobName,
            destinationContainer,
            targetStorageAccount
        );

        blobContainerClientProxy.update(blobName, blobContents, destinationContainer, targetStorageAccount);

        logger.info(
            "Finished uploading file. Blob name: {}. Container: {}. Storage: {}",
            blobName,
            destinationContainer,
            targetStorageAccount
        );
    }
}
