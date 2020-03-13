package uk.gov.hmcts.reform.blobrouter.tasks.processors;

import com.azure.storage.blob.BlobClient;
import com.azure.storage.blob.BlobContainerClient;
import com.azure.storage.blob.BlobServiceClient;
import com.azure.storage.blob.models.BlobItem;
import org.slf4j.Logger;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.reform.blobrouter.data.envelopes.Envelope;
import uk.gov.hmcts.reform.blobrouter.data.envelopes.Status;
import uk.gov.hmcts.reform.blobrouter.services.BlobReadinessChecker;
import uk.gov.hmcts.reform.blobrouter.services.EnvelopeService;

import java.time.Duration;
import java.time.Instant;

import static java.time.Instant.now;
import static org.slf4j.LoggerFactory.getLogger;

@Component
public class ContainerProcessor {

    /**
     * How much time needs to pass before envelope in CREATED status can be considered stale.
     */
    public static final Duration STALE_AGE = Duration.ofMinutes(5);

    private static final Logger logger = getLogger(ContainerProcessor.class);

    private final BlobServiceClient storageClient;
    private final BlobProcessor blobProcessor;
    private final BlobReadinessChecker blobReadinessChecker;
    private final EnvelopeService envelopeService;

    public ContainerProcessor(
        BlobServiceClient storageClient,
        BlobProcessor blobProcessor,
        BlobReadinessChecker blobReadinessChecker,
        EnvelopeService envelopeService
    ) {
        this.storageClient = storageClient;
        this.blobProcessor = blobProcessor;
        this.blobReadinessChecker = blobReadinessChecker;
        this.envelopeService = envelopeService;
    }

    public void process(String containerName) {
        logger.info("Processing container {}", containerName);

        try {
            BlobContainerClient containerClient = storageClient.getBlobContainerClient(containerName);
            containerClient
                .listBlobs()
                .stream()
                .filter(blobItem -> isReady(blobItem, containerName))
                .map(blobItem -> containerClient.getBlobClient(blobItem.getName()))
                .forEach(blob -> processBlob(blob));

            logger.info("Finished processing container {}", containerName);
        } catch (Exception exception) {
            logger.error("Error occurred while processing {} container", containerName, exception);
        }
    }

    private boolean isReady(BlobItem blob, String containerName) {
        Instant blobCreationDate = blob.getProperties().getLastModified().toInstant();
        if (blobReadinessChecker.isReady(blobCreationDate)) {
            return true;
        } else {
            logger.info(
                "Blob not ready to be processed yet, skipping. File name: {}. Container: {}",
                blob.getName(),
                containerName
            );
            return false;
        }
    }

    private void processBlob(BlobClient blob) {
        envelopeService
            .findEnvelope(blob.getBlobName(), blob.getContainerName())
            .ifPresentOrElse(
                envelope -> {
                    if (isStale(envelope)) {
                        logger.warn(
                            "Found stale envelope. Resuming its processing. Envelope ID: {}",
                            envelope.id
                        );
                        blobProcessor.continueProcessing(envelope.id, blob);
                    } else {
                        logger.info(
                            "Envelope already processed in system, skipping."
                                + " ID: {}, filename: {}, container: {}, state: {}",
                            envelope.id,
                            envelope.fileName,
                            envelope.container,
                            envelope.status.name()
                        );
                    }
                },
                () -> blobProcessor.process(blob)
            );
    }

    private boolean isStale(Envelope envelope) {
        return envelope.status == Status.CREATED
            && envelope.createdAt.isBefore(now().minus(STALE_AGE));
    }
}
