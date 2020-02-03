package uk.gov.hmcts.reform.blobrouter.tasks.processors;

import com.azure.storage.blob.BlobClient;
import com.azure.storage.blob.BlobServiceClient;
import com.azure.storage.blob.models.BlobContainerItem;
import com.azure.storage.blob.models.BlobItem;
import com.azure.storage.blob.models.BlobListDetails;
import com.azure.storage.blob.models.ListBlobsOptions;
import org.slf4j.Logger;

import java.time.Duration;

import static java.time.OffsetDateTime.now;
import static org.slf4j.LoggerFactory.getLogger;
import static uk.gov.hmcts.reform.blobrouter.services.storage.RejectedFilesHandler.REJECTED_CONTAINER_SUFFIX;

public class RejectedContainerCleaner {

    private static final Logger logger = getLogger(RejectedContainerCleaner.class);

    private static final ListBlobsOptions listOptions =
        new ListBlobsOptions().setDetails(new BlobListDetails().setRetrieveSnapshots(true));

    private final BlobServiceClient storageClient;
    private final Duration ttl;

    public RejectedContainerCleaner(BlobServiceClient storageClient, Duration ttl) {
        this.storageClient = storageClient;
        this.ttl = ttl;
    }

    public void cleanUp() {
        storageClient
            .listBlobContainers()
            .stream()
            .map(BlobContainerItem::getName)
            .filter(containerName -> containerName.endsWith(REJECTED_CONTAINER_SUFFIX))
            .forEach(containerName -> {
                cleanUpContainer(containerName);
            });
    }

    private void cleanUpContainer(String containerName) {
        logger.info("Looking for rejected files to delete. Container name: {}", containerName);
        var containerClient = storageClient.getBlobContainerClient(containerName);

        containerClient
            .listBlobs(listOptions, null)
            .stream()
            .filter(this::shouldBeDeleted)
            .map(blobItem -> containerClient.getBlobClient(blobItem.getName()))
            .forEach(blobClient -> {
                delete(blobClient);
            });
        logger.info("Finished removing rejected files. Container: {}", containerName);
    }

    private boolean shouldBeDeleted(BlobItem blobItem) {
        return blobItem
            .getProperties()
            .getLastModified()
            .plus(ttl)
            .isBefore(now());
    }

    private void delete(BlobClient blobClient) {
        try {
            blobClient.delete();
            logger.info(
                "Deleted rejected file. File name: {}. Snapshot id: {}",
                blobClient.getBlobName(),
                blobClient.getSnapshotId()
            );
        } catch (Exception exc) {
            logger.error(
                "Error deleting rejected file. Container: {}. File name: {}. Snapshot id: {}",
                blobClient.getContainerName(),
                blobClient.getBlobName(),
                blobClient.getSnapshotId()
            );
        }
    }
}
