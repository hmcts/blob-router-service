package uk.gov.hmcts.reform.blobrouter.tasks.processors;

import com.azure.storage.blob.BlobClient;
import com.azure.storage.blob.BlobContainerClient;
import com.azure.storage.blob.BlobServiceClient;
import org.slf4j.Logger;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.reform.blobrouter.data.envelopes.Envelope;
import uk.gov.hmcts.reform.blobrouter.services.EnvelopeService;
import uk.gov.hmcts.reform.blobrouter.services.storage.LeaseAcquirer;

import java.util.Optional;

import static org.slf4j.LoggerFactory.getLogger;

@Component
public class ContainerProcessor {

    private static final Logger logger = getLogger(ContainerProcessor.class);

    private final BlobServiceClient storageClient;
    private final BlobProcessor blobProcessor;
    private final LeaseAcquirer leaseAcquirer;
    private final EnvelopeService envelopeService;

    public ContainerProcessor(
        BlobServiceClient storageClient,
        BlobProcessor blobProcessor,
        LeaseAcquirer leaseAcquirer,
        EnvelopeService envelopeService
    ) {
        this.storageClient = storageClient;
        this.blobProcessor = blobProcessor;
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
                .map(blobItem -> containerClient.getBlobClient(blobItem.getName()))
                .forEach(blob -> processBlob(blob));

            logger.info("Finished processing container {}", containerName);
        } catch (Exception exception) {
            logger.error("Error occurred while processing {} container", containerName, exception);
        }
    }

    private void processBlob(BlobClient blobClient) {
        findEnvelopeNotInCreatedStatus(blobClient)
            .ifPresentOrElse(
                this::logEnvelopeAlreadyProcessed,
                () -> leaseAndProcess(blobClient)
            );
    }

    private void logEnvelopeAlreadyProcessed(Envelope envelope) {
        logger.info("Envelope already processed in db, skipping. {} ", envelope.getBasicInfo());
    }

    private Optional<Envelope> findEnvelopeNotInCreatedStatus(BlobClient blobClient) {
        return envelopeService
            .findEnvelopeNotInCreatedStatus(blobClient.getBlobName(), blobClient.getContainerName());
    }

    private void leaseAndProcess(BlobClient blobClient) {
        leaseAcquirer.ifAcquiredOrElse(
            blobClient,
            () ->  blobProcessor.process(blobClient),
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
