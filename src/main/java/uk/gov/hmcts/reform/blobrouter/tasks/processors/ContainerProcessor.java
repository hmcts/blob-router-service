package uk.gov.hmcts.reform.blobrouter.tasks.processors;

import com.azure.storage.blob.BlobContainerClient;
import com.azure.storage.blob.BlobServiceClient;
import com.azure.storage.blob.models.BlobItem;
import org.slf4j.Logger;

import static org.slf4j.LoggerFactory.getLogger;

@SuppressWarnings("java:S1135") // ignore TODOs. will be removed when blob processing part is implemented
public class ContainerProcessor {

    private static final Logger logger = getLogger(ContainerProcessor.class);

    private final BlobServiceClient storageClient;

    public ContainerProcessor(BlobServiceClient storageClient) {
        this.storageClient = storageClient;
    }

    public void process(String containerName) {
        logger.info("Processing container {}", containerName);

        try {
            BlobContainerClient containerClient = storageClient.getBlobContainerClient(containerName);

            containerClient
                .listBlobs()
                .forEach(this::processBlob);

            logger.info("Finished processing container {}", containerName);
        } catch (Exception exception) {
            logger.error("Error occurred while processing {} container", containerName, exception);
        }
    }

    @SuppressWarnings("java:S1172")
    // ignore unused variable. This will be removed once the blob processor code is implemented
    private void processBlob(BlobItem blob) {
        // TODO: process blob
    }
}
