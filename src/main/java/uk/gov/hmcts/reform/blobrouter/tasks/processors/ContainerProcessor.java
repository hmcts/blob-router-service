package uk.gov.hmcts.reform.blobrouter.tasks.processors;

import com.azure.storage.blob.BlobContainerAsyncClient;
import com.azure.storage.blob.models.BlobItem;
import org.slf4j.Logger;

import java.util.concurrent.atomic.AtomicInteger;

import static org.slf4j.LoggerFactory.getLogger;

public class ContainerProcessor {

    private static final Logger LOGGER = getLogger(ContainerProcessor.class);

    public void process(BlobContainerAsyncClient containerClient) {
        String containerName = containerClient.getBlobContainerName();
        AtomicInteger processedBlobCount = new AtomicInteger(0);

        LOGGER.info("Processing container {}", containerName);

        containerClient
            .listBlobs()
            .subscribe(
                blob -> {
                    processBlob(blob);
                    processedBlobCount.incrementAndGet();
                },
                null, // TODO: error consumer
                () -> LOGGER.info(
                    "Finished processing container {}. Blobs processed: {}", containerName, processedBlobCount.get()
                )
            );
    }

    private void processBlob(BlobItem blob) {
        // TODO: process blob
    }
}
