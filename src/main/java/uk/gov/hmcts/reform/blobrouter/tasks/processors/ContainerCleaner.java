package uk.gov.hmcts.reform.blobrouter.tasks.processors;

import com.azure.core.util.Context;
import com.azure.storage.blob.BlobClient;
import com.azure.storage.blob.BlobContainerClient;
import com.azure.storage.blob.BlobServiceClient;
import com.azure.storage.blob.models.DeleteSnapshotsOptionType;
import org.slf4j.Logger;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.reform.blobrouter.data.envelopes.Envelope;
import uk.gov.hmcts.reform.blobrouter.services.EnvelopeService;
import uk.gov.hmcts.reform.blobrouter.services.storage.LeaseAcquirer;

import static com.azure.storage.blob.models.BlobErrorCode.BLOB_NOT_FOUND;
import static org.slf4j.LoggerFactory.getLogger;

@Component
public class ContainerCleaner {

    private static final Logger logger = getLogger(ContainerCleaner.class);

    private final BlobServiceClient storageClient;
    private final EnvelopeService envelopeService;
    private final LeaseAcquirer leaseAcquirer;

    public ContainerCleaner(
        BlobServiceClient storageClient,
        EnvelopeService envelopeService,
        LeaseAcquirer leaseAcquirer
    ) {
        this.storageClient = storageClient;
        this.envelopeService = envelopeService;
        this.leaseAcquirer = leaseAcquirer;
    }

    public void process(String containerName) {
        logger.info("Started deleting dispatched blobs from container {}", containerName);


        try {
            final BlobContainerClient containerClient = storageClient.getBlobContainerClient(containerName);

            envelopeService
                .getReadyToDeleteDispatches(containerName)
                .forEach(envelope -> deleteBlob(envelope, containerClient));
        } catch (Exception ex) {
            logger.error("Error deleting blobs in container {}", containerName, ex);
        }

        logger.info("Finished deleting dispatched blobs from container {}", containerName);
    }

    /**
     * The `deleteBlob` function deletes a blob from a BlobContainerClient, handling different
     * scenarios such as acquiring a lease, marking the envelope as deleted if the blob is not found,
     * and logging errors.
     *
     * @param envelope The `envelope` parameter in the `deleteBlob` method likely represents an object
     *                 that contains information about a file or document, such as its file name. It is
     *                 used to identify the blob that needs to be deleted from the `BlobContainerClient`.
     * @param containerClient The `containerClient` parameter is of type `BlobContainerClient` and represents
     *                        a client that interacts with a specific blob container in Azure Blob Storage.
     *                        In the provided code snippet, it is used to get a reference to a specific blob within
     *                        the container for deletion.
     */
    private void deleteBlob(Envelope envelope, BlobContainerClient containerClient) {
        BlobClient blobClient = containerClient.getBlobClient(envelope.fileName);

        leaseAcquirer.ifAcquiredOrElse(
            blobClient,
            () -> tryToDeleteBlob(envelope, blobClient),
            errorCode -> {
                if (BLOB_NOT_FOUND == errorCode) {
                    envelopeService.markEnvelopeAsDeleted(envelope);
                    logger.info(
                        "Marked blob as deleted. File name: {}, container: {}, original error code: {}",
                        envelope.fileName,
                        blobClient.getContainerName(),
                        errorCode
                    );
                } else {
                    logger.error(
                        "Blob delete error,File name: {}, container: {}, original error code: {}",
                        envelope.fileName,
                        blobClient.getContainerName(),
                        errorCode
                    );
                }
            },
            false
        );
    }

    /**
     * The function `tryToDeleteBlob` attempts to delete a blob using a BlobClient and marks an envelope as deleted,
     * logging success or error messages accordingly.
     *
     * @param envelope The `envelope` parameter in the `tryToDeleteBlob` method likely represents
     *                 an object that contains information about a specific envelope. This information
     *                 could include details such as the file name, status, metadata, and any other relevant
     *                 data related to the envelope.
     * @param blobClient BlobClient is a class that represents a client to interact with Azure Blob Storage.
     *                   It provides methods to perform operations like uploading, downloading, and deleting
     *                   blobs in a storage container. In the provided code snippet, the tryToDeleteBlob
     *                   method attempts to delete a blob using the BlobClient instance passed as a parameter.
     */
    private void tryToDeleteBlob(
        Envelope envelope,
        BlobClient blobClient
    ) {
        try {
            blobClient.deleteWithResponse(
                DeleteSnapshotsOptionType.INCLUDE,
                null,
                null,
                Context.NONE
            );
            envelopeService.markEnvelopeAsDeleted(envelope);
            logger.info(
                "Deleted dispatched blob {} from container {}",
                envelope.fileName,
                blobClient.getContainerName()
            );
        } catch (Exception ex) {
            logger.error(
                "Error deleting dispatched blob {} from container {}",
                envelope.fileName,
                blobClient.getContainerName(),
                ex
            );
        }
    }
}
