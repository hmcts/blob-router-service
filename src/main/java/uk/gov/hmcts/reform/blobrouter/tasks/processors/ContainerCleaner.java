package uk.gov.hmcts.reform.blobrouter.tasks.processors;

import com.azure.core.util.Context;
import com.azure.storage.blob.BlobClient;
import com.azure.storage.blob.BlobContainerClient;
import com.azure.storage.blob.BlobServiceClient;
import com.azure.storage.blob.models.BlobRequestConditions;
import com.azure.storage.blob.models.DeleteSnapshotsOptionType;
import org.slf4j.Logger;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.reform.blobrouter.data.envelopes.Envelope;
import uk.gov.hmcts.reform.blobrouter.services.EnvelopeService;
import uk.gov.hmcts.reform.blobrouter.services.storage.LeaseAcquirer;

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

    private void deleteBlob(Envelope envelope, BlobContainerClient containerClient) {
        BlobClient blobClient = containerClient.getBlobClient(envelope.fileName);

        leaseAcquirer.ifAcquiredOrElse(
            blobClient,
            leaseId -> tryToDeleteBlob(envelope, blobClient, leaseId),
            errorCode -> {
                envelopeService.markEnvelopeAsDeleted(envelope);
                logger.info(
                    // once cleared up - i'll create amendment as at this stage we should not care about error code
                    "Marked blob as deleted. File name: {}, container: {}, original error code: {}",
                    envelope.fileName,
                    blobClient.getContainerName(),
                    errorCode
                );
            },
            false
        );
    }

    private void tryToDeleteBlob(
        Envelope envelope,
        BlobClient blobClient,
        String leaseId
    ) {
        try {
            blobClient.deleteWithResponse(
                DeleteSnapshotsOptionType.INCLUDE,
                new BlobRequestConditions(),
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
