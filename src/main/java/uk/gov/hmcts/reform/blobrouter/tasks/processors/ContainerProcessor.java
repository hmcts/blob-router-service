package uk.gov.hmcts.reform.blobrouter.tasks.processors;

import com.azure.storage.blob.BlobContainerAsyncClient;
import com.azure.storage.blob.BlobServiceAsyncClient;
import com.azure.storage.blob.models.BlobItem;
import org.slf4j.Logger;

import static org.slf4j.LoggerFactory.getLogger;

@SuppressWarnings("java:S1135") // ignore TODOs. will be removed when blob processing part is implemented
public class ContainerProcessor {

    private static final Logger logger = getLogger(ContainerProcessor.class);

    private final BlobServiceAsyncClient storageClient;

    public ContainerProcessor(BlobServiceAsyncClient storageClient) {
        this.storageClient = storageClient;
    }

    public void process(String containerName) {
        logger.info("Processing container {}", containerName);

        BlobContainerAsyncClient containerClient = storageClient.getBlobContainerAsyncClient(containerName);

        containerClient
            .listBlobs()
            .subscribe(
                this::processBlob,
                throwable -> logger.error("Error occurred while processing blob", throwable), // TODO: error consumer
                () -> logger.info(
                    "Finished processing container {}", containerName
                )
            );
    }

    @SuppressWarnings("java:S1172")
    // ignore unused variable. This will be removed once the blob processor code is implemented
    private void processBlob(BlobItem blob) {
        // TODO: process blob
    }
}
