package uk.gov.hmcts.reform.blobrouter.tasks.processors;

import com.azure.storage.blob.BlobAsyncClient;
import com.azure.storage.blob.BlobContainerAsyncClient;
import com.azure.storage.blob.models.BlobItem;
import com.azure.storage.blob.specialized.BlobLeaseAsyncClient;
import com.azure.storage.blob.specialized.BlobLeaseClientBuilder;
import org.slf4j.Logger;

import java.util.concurrent.atomic.AtomicInteger;

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
        AtomicInteger atomInt = new AtomicInteger(0);

        LOGGER.info("Processing container {}", containerName);

        containerClient
            .listBlobs()
            .subscribe(
                blob -> {
                    processBlobs(containerClient, blob);
                    atomInt.updateAndGet(operand -> ++operand);
                },
                null, // error consumer. can be implemented later
                () -> LOGGER.info("Finished processing container {}. Blobs found: {}", containerName, atomInt.get())
            );
    }

    private void processBlobs(BlobContainerAsyncClient containerClient, BlobItem blob) {
        BlobAsyncClient blobClient = containerClient.getBlobAsyncClient(blob.getName());
        BlobLeaseAsyncClient blobLeaseClient = LEASE_CLIENT_BUILDER
            .blobAsyncClient(blobClient)
            .buildAsyncClient();

        blobProcessor.process(blobClient, blobLeaseClient, blob);
    }
}
