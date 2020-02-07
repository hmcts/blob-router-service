package uk.gov.hmcts.reform.blobrouter.tasks.processors;

import com.azure.storage.blob.BlobClient;
import com.azure.storage.blob.BlobServiceClient;
import com.azure.storage.blob.models.BlobContainerItem;
import com.azure.storage.blob.models.BlobListDetails;
import com.azure.storage.blob.models.ListBlobsOptions;
import org.slf4j.Logger;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.reform.blobrouter.services.RejectedBlobChecker;

import static org.slf4j.LoggerFactory.getLogger;
import static uk.gov.hmcts.reform.blobrouter.services.storage.RejectedFilesHandler.REJECTED_CONTAINER_SUFFIX;

@Component
public class RejectedContainerCleaner {

    private static final Logger logger = getLogger(RejectedContainerCleaner.class);

    private static final ListBlobsOptions listOptions =
        new ListBlobsOptions().setDetails(new BlobListDetails().setRetrieveSnapshots(true));

    private final BlobServiceClient storageClient;
    private final RejectedBlobChecker blobChecker;

    // region constructor
    public RejectedContainerCleaner(
        BlobServiceClient storageClient,
        RejectedBlobChecker blobChecker
    ) {
        this.storageClient = storageClient;
        this.blobChecker = blobChecker;
    }
    // endregion

    public void cleanUp() {
        storageClient
            .listBlobContainers()
            .stream()
            .map(BlobContainerItem::getName)
            .filter(containerName -> containerName.endsWith(REJECTED_CONTAINER_SUFFIX))
            .forEach(this::cleanUpContainer);
    }

    private void cleanUpContainer(String containerName) {
        logger.info("Looking for rejected files to delete. Container: {}", containerName);
        var containerClient = storageClient.getBlobContainerClient(containerName);

        containerClient
            .listBlobs(listOptions, null)
            .stream()
            .filter(this.blobChecker::shouldBeDeleted)
            .map(blobItem -> containerClient.getBlobClient(blobItem.getName()))
            .forEach(this::delete);

        logger.info("Finished removing rejected files. Container: {}", containerName);
    }

    private void delete(BlobClient blobClient) {
        String blobInfo = String.format(
            "Container: %s. File name: %s. Snapshot ID: %s",
            blobClient.getContainerName(),
            blobClient.getBlobName(),
            blobClient.getSnapshotId()
        );

        try {
            blobClient.delete();
            logger.info("Deleted rejected file. {}", blobInfo);
        } catch (Exception exc) {
            logger.error("Error deleting rejected file. {}", blobInfo, exc);
        }
    }
}
