package uk.gov.hmcts.reform.blobrouter.services.storage;

import com.azure.storage.blob.BlobContainerAsyncClient;
import com.azure.storage.blob.BlobServiceAsyncClient;
import com.azure.storage.blob.models.BlockBlobItem;
import org.slf4j.Logger;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.nio.ByteBuffer;

import static org.slf4j.LoggerFactory.getLogger;

public class BlobDispatcher {

    private static final Logger logger = getLogger(BlobDispatcher.class);

    private final BlobServiceAsyncClient storageClient;

    public BlobDispatcher(BlobServiceAsyncClient storageClient) {
        this.storageClient = storageClient;
    }

    public Mono<BlockBlobItem> dispatch(String blobName, byte[] blobContents, String destinationContainer) {
        logger.info("Uploading {} to {} container", blobName, destinationContainer);

        return getContainerClient(destinationContainer)
            .getBlobAsyncClient(blobName)
            .getBlockBlobAsyncClient()
            .upload(
                Flux.just(ByteBuffer.wrap(blobContents)),
                blobContents.length,
                false // overwrite flag
            )
            .doOnError(error -> logger.error(
                "Error occurred while uploading {} to {} container",
                blobName,
                destinationContainer,
                error
            ))
            .doOnSuccess(uploadedBlob ->
                logger.info("Finished uploading {} to {} container", blobName, destinationContainer)
            );
    }

    // will use different storageClient depending on container
    private BlobContainerAsyncClient getContainerClient(String destinationContainer) {
        return storageClient.getBlobContainerAsyncClient(destinationContainer);
    }
}
