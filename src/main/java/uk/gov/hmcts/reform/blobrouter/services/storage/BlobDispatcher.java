package uk.gov.hmcts.reform.blobrouter.services.storage;

import org.slf4j.Logger;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.reform.blobrouter.config.TargetStorageAccount;

import java.io.ByteArrayInputStream;

import static org.slf4j.LoggerFactory.getLogger;

@Component
public class BlobDispatcher {

    private static final Logger logger = getLogger(BlobDispatcher.class);

    private final BlobContainerClientProvider blobContainerClientProvider;

    public BlobDispatcher(BlobContainerClientProvider blobContainerClientProvider) {
        this.blobContainerClientProvider = blobContainerClientProvider;
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

        blobContainerClientProvider
            .get(targetStorageAccount, destinationContainer)
            .getBlobClient(blobName)
            .getBlockBlobClient()
            .upload(
                new ByteArrayInputStream(blobContents),
                blobContents.length
            );

        logger.info(
            "Finished uploading file. Blob name: {}. Container: {}. Storage: {}",
            blobName,
            destinationContainer,
            targetStorageAccount
        );
    }
}
