package uk.gov.hmcts.reform.blobrouter.tasks.processors;

import com.azure.storage.blob.BlobClient;
import com.azure.storage.blob.BlobContainerClient;
import com.azure.storage.blob.BlobServiceClient;
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

        BlobContainerClient containerClient = storageClient.getBlobContainerClient(containerName);

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

        if (blob == null) {
            logger.error(
                String.format(
                    "Blob %s does not exist in container %s",
                    envelope.fileName,
                    containerClient.getBlobContainerName()
                )
            );
        } else {
            try {
                blob.delete();
                envelopeRepository.markAsDeleted(envelope.id);
                logger.info(
                    "Deleted dispatched blob {} from container {}",
                    envelope.fileName,
                    containerClient.getBlobContainerName()
                );
            } catch (Throwable ex) {
                logger.error("--------------");
                ex.printStackTrace();
                logger.error("--------------");
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
    }
}
