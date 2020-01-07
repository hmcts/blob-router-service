package uk.gov.hmcts.reform.blobrouter.tasks.processors;

import com.azure.storage.blob.BlobContainerAsyncClient;
import com.azure.storage.blob.models.BlobItem;
import org.slf4j.Logger;

import java.util.concurrent.atomic.AtomicInteger;

import static org.slf4j.LoggerFactory.getLogger;

@SuppressWarnings("java:S1135") // ignore TODOs. will be removed when blob processing part is implemented
public class ContainerProcessor {

    private static final Logger logger = getLogger(ContainerProcessor.class);

    public void process(BlobContainerAsyncClient containerClient) {
        String containerName = containerClient.getBlobContainerName();
        AtomicInteger processedBlobCount = new AtomicInteger(0);

        logger.info("Processing container {}", containerName);

        containerClient
            .listBlobs()
            .subscribe(
                blob -> {
                    processBlob(blob);
                    processedBlobCount.incrementAndGet();
                },
                null, // TODO: error consumer
                () -> logger.info(
                    "Finished processing container {}. Blobs processed: {}", containerName, processedBlobCount.get()
                )
            );
    }

    @SuppressWarnings("java:S1172")
    // ignore unused variable. This will be removed once the blob processor code is implemented
    private void processBlob(BlobItem blob) {
        // TODO: process blob
    }
}
