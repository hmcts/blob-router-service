package uk.gov.hmcts.reform.blobrouter.tasks.processors;

import com.azure.storage.blob.BlobClient;
import com.azure.storage.blob.BlobContainerClient;
import com.azure.storage.blob.BlobServiceClient;
import com.azure.storage.blob.models.BlobStorageException;
import org.apache.http.HttpStatus;
import org.slf4j.Logger;
import uk.gov.hmcts.reform.blobrouter.data.EnvelopeRepository;
import uk.gov.hmcts.reform.blobrouter.data.model.Envelope;

import static org.slf4j.LoggerFactory.getLogger;
import static uk.gov.hmcts.reform.blobrouter.data.model.Status.DISPATCHED;

public class ContainerCleaner {

    private static final Logger logger = getLogger(ContainerCleaner.class);

    private final BlobServiceClient storageClient;
    private final EnvelopeRepository envelopeRepository;

    public ContainerCleaner(
        BlobServiceClient storageClient,
        EnvelopeRepository envelopeRepository
    ) {
        this.storageClient = storageClient;
        this.envelopeRepository = envelopeRepository;
    }

    public void process(String containerName) {
        logger.info("Started deleting dispatched blobs from container {}", containerName);

        final BlobContainerClient containerClient = storageClient.getBlobContainerClient(containerName);

        envelopeRepository
            .find(DISPATCHED, false)
            .forEach(envelope -> {
                tryToDeleteBlob(envelope, containerClient);
            });

        logger.info("Finished deleting dispatched blobs from container {}", containerName);
    }

    private void tryToDeleteBlob(
        Envelope envelope,
        BlobContainerClient containerClient
    ) {
        BlobClient blob = containerClient.getBlobClient(envelope.fileName);

        try {
            blob.delete();
            envelopeRepository.markAsDeleted(envelope.id);
            logger.info(
                "Deleted dispatched blob {} from container {}",
                envelope.fileName,
                containerClient.getBlobContainerName()
            );
        } catch (BlobStorageException ex) {
            if (ex.getStatusCode() == HttpStatus.SC_NOT_FOUND) {
                logger.error(
                    String.format(
                        "Blob %s does not exist in container %s",
                        envelope.fileName,
                        containerClient.getBlobContainerName()
                    ),
                    ex
                );
                envelopeRepository.markAsDeleted(envelope.id);
            } else {
                logException(envelope, containerClient, ex);
            }
        } catch (Exception ex) {
            logException(envelope, containerClient, ex);
        }
    }

    private void logException(Envelope envelope, BlobContainerClient containerClient, Exception ex) {
        logger.error(
            String.format(
                "Error deleting dispatched blob %s from container %s",
                envelope.fileName,
                containerClient.getBlobContainerName()
            ),
            ex
        );
    }
}
