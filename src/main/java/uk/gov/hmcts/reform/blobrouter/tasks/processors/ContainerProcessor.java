package uk.gov.hmcts.reform.blobrouter.tasks.processors;

import com.azure.storage.blob.BlobAsyncClient;
import com.azure.storage.blob.BlobContainerAsyncClient;
import com.azure.storage.blob.models.BlobItem;
import com.azure.storage.blob.specialized.BlobLeaseAsyncClient;
import com.azure.storage.blob.specialized.BlobLeaseClientBuilder;
import org.slf4j.Logger;

import java.util.List;

import static org.slf4j.LoggerFactory.getLogger;

public class ContainerProcessor {

    private static final Logger LOGGER = getLogger(ContainerProcessor.class);

    private static final BlobLeaseClientBuilder LEASE_CLIENT_BUILDER = new BlobLeaseClientBuilder();

    private final BlobProcessor blobProcessor;

    public ContainerProcessor(BlobProcessor blobProcessor) {
        this.blobProcessor = blobProcessor;
    }

    public void process(BlobContainerAsyncClient containerClient) {
        String containerName = containerClient.getBlobContainerName();

        LOGGER.info("Processing container {}", containerName);

        containerClient
            .listBlobs()
            .buffer()
            .subscribe(
                blobs -> processBlobs(containerClient, blobs),
                null,
                () -> LOGGER.info("Finished processing container {}", containerName)
            );
    }

    private void processBlobs(
        BlobContainerAsyncClient containerClient,
        List<BlobItem> blobs
    ) {
        long blobsFound = blobs
            .stream()
            .mapToInt(item -> {
                BlobAsyncClient blobClient = containerClient.getBlobAsyncClient(item.getName());
                BlobLeaseAsyncClient blobLeaseClient = LEASE_CLIENT_BUILDER
                    .blobAsyncClient(blobClient)
                    .buildAsyncClient();

                blobProcessor.process(blobClient, blobLeaseClient, item);

                return 1;
            })
            .sum();

        LOGGER.info(
            "Blobs found in {} container: {}",
            containerClient.getBlobContainerName(),
            blobsFound
        );
    }
}
