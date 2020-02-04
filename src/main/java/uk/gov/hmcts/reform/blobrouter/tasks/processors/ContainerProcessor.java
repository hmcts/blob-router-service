package uk.gov.hmcts.reform.blobrouter.tasks.processors;

import com.azure.storage.blob.BlobContainerClient;
import com.azure.storage.blob.BlobServiceClient;
import org.slf4j.Logger;
import org.springframework.stereotype.Component;

import static org.slf4j.LoggerFactory.getLogger;

@Component
public class ContainerProcessor {

    private static final Logger logger = getLogger(ContainerProcessor.class);

    private final BlobServiceClient storageClient;
    private final BlobProcessor blobProcessor;

    public ContainerProcessor(
        BlobServiceClient storageClient,
        BlobProcessor blobProcessor
    ) {
        this.storageClient = storageClient;
        this.blobProcessor = blobProcessor;
    }

    public void process(String containerName) {
        logger.info("Processing container {}", containerName);

        try {
            BlobContainerClient containerClient = storageClient.getBlobContainerClient(containerName);

            containerClient
                .listBlobs()
                .forEach(blob -> blobProcessor.process(blob.getName(), containerName));

            logger.info("Finished processing container {}", containerName);
        } catch (Exception exception) {
            logger.error("Error occurred while processing {} container", containerName, exception);
        }
    }
}
