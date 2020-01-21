package uk.gov.hmcts.reform.blobrouter.tasks.processors;

import com.azure.storage.blob.BlobClient;
import com.azure.storage.blob.BlobServiceClient;
import com.azure.storage.blob.models.BlobStorageException;
import com.azure.storage.blob.specialized.BlobLeaseClient;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.reform.blobrouter.config.TargetStorageAccount;
import uk.gov.hmcts.reform.blobrouter.data.EnvelopeRepository;
import uk.gov.hmcts.reform.blobrouter.data.model.NewEnvelope;
import uk.gov.hmcts.reform.blobrouter.data.model.Status;
import uk.gov.hmcts.reform.blobrouter.services.BlobReadinessChecker;
import uk.gov.hmcts.reform.blobrouter.services.storage.BlobDispatcher;
import uk.gov.hmcts.reform.blobrouter.services.storage.LeaseClientProvider;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.Instant;
import java.util.UUID;

import static org.slf4j.LoggerFactory.getLogger;

@Component
public class BlobProcessor {

    private static final Logger logger = getLogger(BlobProcessor.class);

    private final BlobServiceClient storageClient;
    private final BlobDispatcher dispatcher;
    private final BlobReadinessChecker readinessChecker;
    private final EnvelopeRepository envelopeRepository;
    private final LeaseClientProvider leaseClientProvider;

    public BlobProcessor(
        @Qualifier("storage-client") BlobServiceClient storageClient,
        BlobDispatcher dispatcher,
        BlobReadinessChecker readinessChecker,
        EnvelopeRepository envelopeRepository,
        LeaseClientProvider leaseClientProvider
    ) {
        this.storageClient = storageClient;
        this.dispatcher = dispatcher;
        this.readinessChecker = readinessChecker;
        this.envelopeRepository = envelopeRepository;
        this.leaseClientProvider = leaseClientProvider;
    }

    public void process(String blobName, String containerName) {
        envelopeRepository
            .find(blobName, containerName)
            .ifPresentOrElse(
                envelope -> logger.info(
                    "Envelope already processed in system, skipping. ID: {}, filename: {}, container: {}, state: {}",
                    envelope.id,
                    envelope.fileName,
                    envelope.container,
                    envelope.status.name()
                ),
                () -> processBlob(blobName, containerName)
            );
    }

    private void processBlob(String blobName, String containerName) {
        logger.info("Processing {} from {} container", blobName, containerName);

        BlobLeaseClient leaseClient = null;

        try {
            BlobClient blobClient = storageClient
                .getBlobContainerClient(containerName)
                .getBlobClient(blobName);

            Instant blobCreationDate = blobClient.getProperties().getCreationTime().toInstant();

            if (this.readinessChecker.isReady(blobCreationDate)) {
                leaseClient = leaseClientProvider.get(blobClient);

                leaseClient.acquireLease(60);
                byte[] rawBlob = tryToDownloadBlob(blobClient);

                // target storage account will be retrieved from configuration when Crime blob processing is supported
                dispatcher.dispatch(blobName, rawBlob, containerName, TargetStorageAccount.BULKSCAN);
                UUID envelopeId = envelopeRepository.insert(createNewEnvelope(
                    blobName,
                    containerName,
                    blobCreationDate
                ));

                logger.info(
                    "Finished processing {} from {} container. New envelope ID: {}",
                    blobName,
                    containerName,
                    envelopeId
                );
            } else {
                logger.info(
                    "Blob not ready to be processed yet, skipping. File name: {}. Container: {}",
                    blobName,
                    containerName
                );
            }
        } catch (Exception exception) {
            logger.error("Error occurred while processing {} from {}", blobName, containerName, exception);
        } finally {
            tryToReleaseLease(leaseClient, blobName, containerName);
        }
    }

    private byte[] tryToDownloadBlob(BlobClient blobClient) throws IOException {
        try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            blobClient.download(outputStream);

            return outputStream.toByteArray();
        }
    }

    private void tryToReleaseLease(BlobLeaseClient leaseClient, String blobName, String containerName) {
        try {
            if (leaseClient != null) {
                leaseClient.releaseLease();
            }
        } catch (BlobStorageException exception) {
            // this will mean there was a problem acquiring lease in the first place
            // or call to release the lease genuinely went wrong.
            // warning as lease will expire anyway and normally should sort itself out
            logger.warn(
                "Could not release the lease with ID {}. Blob: {}, container: {}",
                leaseClient.getLeaseId(),
                blobName,
                containerName,
                exception
            );
        }
    }

    private NewEnvelope createNewEnvelope(String blobName, String containerName, Instant fileCreatedAt) {
        return new NewEnvelope(
            containerName,
            blobName,
            fileCreatedAt,
            Instant.now(),
            Status.DISPATCHED
        );
    }
}
