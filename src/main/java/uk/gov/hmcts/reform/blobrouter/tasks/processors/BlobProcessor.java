package uk.gov.hmcts.reform.blobrouter.tasks.processors;

import com.azure.storage.blob.BlobClient;
import com.azure.storage.blob.BlobServiceClient;
import com.azure.storage.blob.specialized.BlobLeaseClient;
import com.azure.storage.blob.specialized.BlobLeaseClientBuilder;
import org.slf4j.Logger;
import uk.gov.hmcts.reform.blobrouter.services.storage.BlobDispatcher;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

import static org.slf4j.LoggerFactory.getLogger;

public class BlobProcessor {

    private static final Logger logger = getLogger(BlobProcessor.class);

    private final BlobServiceClient storageClient;
    private final BlobDispatcher dispatcher;

    public BlobProcessor(
        BlobServiceClient storageClient,
        BlobDispatcher dispatcher
    ) {
        this.storageClient = storageClient;
        this.dispatcher = dispatcher;
    }

    public void process(String blobName, String containerName) {
        logger.info("Processing {} from {} container", blobName, containerName);

        BlobLeaseClient leaseClient = null;

        try {
            BlobClient blobClient = storageClient
                .getBlobContainerClient(containerName)
                .getBlobClient(blobName);
            leaseClient = new BlobLeaseClientBuilder()
                .blobClient(blobClient)
                .buildClient();

            leaseClient.acquireLease(60);
            byte[] rawBlob = tryToDownloadBlob(blobClient);

            dispatcher.dispatch(blobName, rawBlob, containerName);

            logger.info("Finished processing {} from {} container", blobName, containerName);
        } catch (Exception exception) {
            logger.error("Error occurred while processing {} from {}", blobName, containerName, exception);
        } finally {
            if (leaseClient != null) {
                leaseClient.releaseLease();
            }
        }
    }

    public byte[] tryToDownloadBlob(BlobClient blobClient) throws IOException {
        try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            blobClient.download(outputStream);

            return outputStream.toByteArray();
        }
    }
}
