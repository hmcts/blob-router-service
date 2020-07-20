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
import uk.gov.hmcts.reform.blobrouter.services.storage.LeaseAcquirer;

import java.time.Instant;
import java.util.Optional;

import static org.slf4j.LoggerFactory.getLogger;

@Component
public class ContainerProcessor {

    private static final Logger logger = getLogger(ContainerProcessor.class);

    private final BlobServiceClient storageClient;
    private final BlobProcessor blobProcessor;
    private final BlobReadinessChecker blobReadinessChecker;
    private final LeaseAcquirer leaseAcquirer;
    private final EnvelopeService envelopeService;

    public ContainerProcessor(
        BlobServiceClient storageClient,
        BlobProcessor blobProcessor,
        BlobReadinessChecker blobReadinessChecker,
        LeaseAcquirer leaseAcquirer,
        EnvelopeService envelopeService
    ) {
        this.storageClient = storageClient;
        this.blobProcessor = blobProcessor;
        this.blobReadinessChecker = blobReadinessChecker;
        this.leaseAcquirer = leaseAcquirer;
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
        getLastEnvelope(blob)
            .ifPresentOrElse(
                envelope -> {
                    if (envelope.status == Status.CREATED) {
                        leaseAndThen(blob, () -> {
                            getLastEnvelope(blob)
                                .ifPresentOrElse(
                                    envelopePotentiallyProcessed -> {
                                        continueProcessingIfPossible(blob, envelopePotentiallyProcessed);
                                    },
                                    () -> logger.error(
                                        "Envelope no longer exists in system for blob {}, container {}",
                                        blob.getBlobName(), blob.getContainerName()
                                    )
                                );
                        });
                    } else {
                        logEnvelopeAlreadyProcessed(envelope);
                    }
                },
                () -> leaseAndThen(blob, () -> {
                    getLastEnvelope(blob)
                        .ifPresentOrElse(
                            envelopeAlreadyExisting -> {
                                continueProcessingIfPossible(blob, envelopeAlreadyExisting);
                            },
                            () -> blobProcessor.process(blob)
                        );
                })
            );
    }

    private void continueProcessingIfPossible(BlobClient blob, Envelope envelope) {
        if (envelope.status == Status.CREATED) {
            blobProcessor.continueProcessing(envelope.id, blob);
        } else {
            logEnvelopeAlreadyProcessed(envelope);
        }
    }

    private void logEnvelopeAlreadyProcessed(Envelope envelope) {
        logger.info("Envelope already processed in system, skipping. {} ", envelope.getBasicInfo());
    }

    private Optional<Envelope> getLastEnvelope(BlobClient blob) {
        return envelopeService
            .findLastEnvelope(blob.getBlobName(), blob.getContainerName());
    }

    private void leaseAndThen(BlobClient blobClient, Runnable action) {
        leaseAcquirer.ifAcquiredOrElse(
            blobClient,
            leaseId -> action.run(),
            errorCode -> logger.info(
                "Cannot acquire a lease for blob - skipping. File name: {}, container: {}, error code: {}",
                blobClient.getBlobName(),
                blobClient.getContainerName(),
                errorCode
            ),
            true
        );
    }
}
