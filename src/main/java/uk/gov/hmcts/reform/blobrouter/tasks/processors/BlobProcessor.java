package uk.gov.hmcts.reform.blobrouter.tasks.processors;

import com.azure.storage.blob.BlobAsyncClient;
import com.azure.storage.blob.models.BlobItem;
import com.azure.storage.blob.specialized.BlobLeaseAsyncClient;
import org.slf4j.Logger;

import static org.slf4j.LoggerFactory.getLogger;

public class BlobProcessor {

    private static final Logger LOGGER = getLogger(BlobProcessor.class);

    public BlobProcessor() {
    }

    public void process(
        BlobAsyncClient blobClient,
        BlobLeaseAsyncClient blobLeaseClient,
        BlobItem blobItem
    ) {
        String blobName = blobItem.getName();
        String containerName = blobClient.getContainerName();

        LOGGER.info("Processing blob {} from container {}", blobName, containerName);

        // TODO: implement
    }
}
