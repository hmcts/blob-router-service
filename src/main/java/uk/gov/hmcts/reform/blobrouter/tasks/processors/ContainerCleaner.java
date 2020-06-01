package uk.gov.hmcts.reform.blobrouter.tasks.processors;

import com.azure.storage.blob.BlobClient;
import com.azure.storage.blob.BlobContainerClient;
import com.azure.storage.blob.BlobServiceClient;
import com.azure.storage.blob.models.BlobStorageException;
import org.apache.http.HttpStatus;
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
                .forEach(envelope -> {
                    deleteBlob(envelope, containerClient);
                });
        } catch (Exception ex) {
            logger.error("Error deleting blobs in container {}", containerName, ex);
        }

        logger.info("Finished deleting dispatched blobs from container {}", containerName);
    }

    private void deleteBlob(Envelope envelope, BlobContainerClient containerClient) {
        var blobClient = containerClient.getBlobClient(envelope.fileName);

        leaseAcquirer.ifAcquiredOrElse(
            blobClient,
            () -> tryToDeleteBlob(envelope, blobClient),
            () -> {} // no need to report error here
        );
    }

    private void tryToDeleteBlob(Envelope envelope, BlobClient blobClient) {
        try {
            blobClient.delete();
            envelopeService.markEnvelopeAsDeleted(envelope);
            logger.info(
                "Deleted dispatched blob {} from container {}",
                envelope.fileName,
                blobClient.getContainerName()
            );
        } catch (BlobStorageException ex) {
            if (ex.getStatusCode() == HttpStatus.SC_NOT_FOUND) {
                logger.error(
                    String.format(
                        "Blob %s does not exist in container %s",
                        envelope.fileName,
                        blobClient.getContainerName()
                    ),
                    ex
                );
                envelopeService.markEnvelopeAsDeleted(envelope);
            } else {
                logException(envelope, blobClient, ex);
            }
        } catch (Exception ex) {
            logException(envelope, blobClient, ex);
        }
    }

    private void logException(Envelope envelope, BlobClient blobClient, Exception ex) {
        logger.error(
            String.format(
                "Error deleting dispatched blob %s from container %s",
                envelope.fileName,
                blobClient.getContainerName()
            ),
            ex
        );
    }
}
