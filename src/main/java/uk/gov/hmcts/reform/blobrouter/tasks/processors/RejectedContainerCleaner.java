package uk.gov.hmcts.reform.blobrouter.tasks.processors;

import com.azure.core.util.Context;
import com.azure.storage.blob.BlobClient;
import com.azure.storage.blob.BlobServiceClient;
import com.azure.storage.blob.models.BlobContainerItem;
import com.azure.storage.blob.models.BlobListDetails;
import com.azure.storage.blob.models.BlobRequestConditions;
import com.azure.storage.blob.models.DeleteSnapshotsOptionType;
import com.azure.storage.blob.models.ListBlobsOptions;
import org.slf4j.Logger;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.reform.blobrouter.data.events.EventType;
import uk.gov.hmcts.reform.blobrouter.services.EnvelopeService;
import uk.gov.hmcts.reform.blobrouter.services.RejectedBlobChecker;
import uk.gov.hmcts.reform.blobrouter.services.storage.LeaseAcquirer;

import static org.slf4j.LoggerFactory.getLogger;
import static uk.gov.hmcts.reform.blobrouter.services.storage.RejectedFilesHandler.REJECTED_CONTAINER_SUFFIX;

@Component
public class RejectedContainerCleaner {

    private static final Logger logger = getLogger(RejectedContainerCleaner.class);

    private static final ListBlobsOptions listOptions =
        new ListBlobsOptions().setDetails(new BlobListDetails().setRetrieveSnapshots(true));

    private final BlobServiceClient storageClient;
    private final RejectedBlobChecker blobChecker;
    private final EnvelopeService envelopeService;
    private final LeaseAcquirer leaseAcquirer;

    // region constructor
    public RejectedContainerCleaner(
        BlobServiceClient storageClient,
        RejectedBlobChecker blobChecker,
        EnvelopeService envelopeService,
        LeaseAcquirer leaseAcquirer
    ) {
        this.storageClient = storageClient;
        this.blobChecker = blobChecker;
        this.envelopeService = envelopeService;
        this.leaseAcquirer = leaseAcquirer;
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
            .forEach(blobClient -> leaseAcquirer.ifAcquiredOrElse(
                blobClient,
                leaseId -> delete(blobClient, leaseId),
                errorCode -> {} // nothing to do if blob not found in rejected container
            ));

        logger.info("Finished removing rejected files. Container: {}", containerName);
    }

    private void delete(BlobClient blobClient, String leaseId) {
        String containerName = blobClient.getContainerName();
        String blobName = blobClient.getBlobName();

        String blobInfo = String.format(
            "Container: %s. File name: %s. Snapshot ID: %s",
            containerName,
            blobName,
            blobClient.getSnapshotId()
        );

        try {
            // every time a duplicate is moved to rejected container
            // a snapshot is created and original blob is replaced,
            // therefore snapshots are always older than the 'base' blob and it is safe to delete them
            blobClient.deleteWithResponse(
                DeleteSnapshotsOptionType.INCLUDE,
                new BlobRequestConditions().setLeaseId(leaseId),
                null,
                Context.NONE
            );

            envelopeService
                .findLastEnvelope(blobName, containerName)
                .ifPresentOrElse(
                    e -> envelopeService.saveEvent(e.id, EventType.DELETED_FROM_REJECTED),
                    () -> logger.warn("Envelope not found. {}", blobInfo)
                );
            logger.info("Deleted rejected file. {}", blobInfo);
        } catch (Exception exc) {
            logger.error("Error deleting rejected file. {}", blobInfo, exc);
        }
    }
}
