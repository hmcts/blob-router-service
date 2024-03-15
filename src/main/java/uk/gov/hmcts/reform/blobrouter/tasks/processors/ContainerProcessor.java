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

/**
 * The `ContainerProcessor` class in Java processes blobs in a specified container, logging errors and handling envelope
 * processing based on status and lease acquisition.
 */
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

    /**
     * The `process` method processes all blobs in a specified container and logs any errors that occur.
     *
     * @param containerName The `process` method takes a `containerName` as a parameter, which is used
     *                      to process the blobs within the specified blob container. The method first
     *                      logs a message indicating that it is processing the container with the
     *                      provided name.
     */
    public void process(String containerName) {
        logger.info("Processing container {}", containerName);

        try {
            BlobContainerClient containerClient = storageClient.getBlobContainerClient(containerName);
            containerClient
                .listBlobs()
                .stream()
                .map(blobItem -> containerClient.getBlobClient(blobItem.getName()))
                .forEach(this::processBlob);

            logger.info("Finished processing container {}", containerName);
        } catch (Exception exception) {
            logger.error("Error occurred while processing {} container", containerName, exception);
        }
    }

    /**
     * The `processBlob` function checks if an envelope is not in the created status, logs if it has already been
     * processed, and otherwise leases and processes the blob.
     *
     * @param blobClient BlobClient is a class representing a client to interact with Azure Blob Storage.
     *                   It provides methods to upload, download, delete, and manage blobs in Azure Blob Storage.
     *                   In the given code snippet, the `processBlob` method takes a `BlobClient` as a
     *                   parameter and performs some operations on it.
     */
    private void processBlob(BlobClient blobClient) {
        findEnvelopeNotInCreatedStatus(blobClient)
            .ifPresentOrElse(
                this::logEnvelopeAlreadyProcessed,
                () -> leaseAndProcess(blobClient)
            );
    }

    /**
     * This function logs a message indicating that an envelope has already been processed in the database and will be
     * skipped.
     *
     * @param envelope The `envelope` parameter is an object of type `Envelope`, which likely contains
     *                 information about an envelope being processed. The method `getBasicInfo()` is called on
     *                 the `envelope` object to retrieve basic information about the envelope.
     */
    private void logEnvelopeAlreadyProcessed(Envelope envelope) {
        logger.info("Envelope already processed in db, skipping. {} ", envelope.getBasicInfo());
    }

    /**
     * This function returns an Optional containing an Envelope object that is not in the "created" status based on the
     * BlobClient's blob name and container name.
     *
     * @param blobClient BlobClient blobClient is an object representing a client for interacting with
     *                   Azure Blob Storage. It typically contains information such as the blob name and
     *                   container name.
     * @return An Optional object containing an Envelope object is being returned.
     */
    private Optional<Envelope> findEnvelopeNotInCreatedStatus(BlobClient blobClient) {
        return envelopeService
            .findEnvelopeNotInCreatedStatus(blobClient.getBlobName(), blobClient.getContainerName());
    }

    /**
     * The `leaseAndProcess` method attempts to acquire a lease for a BlobClient and
     * processes it if the lease is acquired, otherwise logs a message indicating the failure to acquire the lease.
     *
     * @param blobClient BlobClient is an object representing a blob in Azure Blob Storage.
     *                   It contains information such as the blob name and the container name where the blob is stored.
     */
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
