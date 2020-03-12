package uk.gov.hmcts.reform.blobrouter.tasks.processors;

import com.azure.storage.blob.BlobClient;
import com.azure.storage.blob.models.BlobStorageException;
import com.azure.storage.blob.specialized.BlobLeaseClient;
import org.slf4j.Logger;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.reform.blobrouter.config.ServiceConfiguration;
import uk.gov.hmcts.reform.blobrouter.config.StorageConfigItem;
import uk.gov.hmcts.reform.blobrouter.config.TargetStorageAccount;
import uk.gov.hmcts.reform.blobrouter.data.events.EventType;
import uk.gov.hmcts.reform.blobrouter.exceptions.ZipFileLoadException;
import uk.gov.hmcts.reform.blobrouter.services.BlobContentExtractor;
import uk.gov.hmcts.reform.blobrouter.services.BlobVerifier;
import uk.gov.hmcts.reform.blobrouter.services.EnvelopeService;
import uk.gov.hmcts.reform.blobrouter.services.storage.BlobDispatcher;
import uk.gov.hmcts.reform.blobrouter.services.storage.LeaseAcquirer;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Map;
import java.util.UUID;
import java.util.function.Supplier;

import static org.slf4j.LoggerFactory.getLogger;
import static org.springframework.http.HttpStatus.BAD_GATEWAY;

@Component
@EnableConfigurationProperties(ServiceConfiguration.class)
public class BlobProcessor {

    private static final Logger logger = getLogger(BlobProcessor.class);

    private static final String MESSAGE_FAILED_TO_DOWNLOAD_BLOB = "Failed to download blob";

    private final BlobDispatcher dispatcher;
    private final EnvelopeService envelopeService;
    private final LeaseAcquirer leaseAcquirer;
    private final BlobVerifier blobVerifier;
    private final BlobContentExtractor blobContentExtractor;
    private final Map<String, StorageConfigItem> storageConfig; // container-specific configuration, by container name

    public BlobProcessor(
        BlobDispatcher dispatcher,
        EnvelopeService envelopeService,
        LeaseAcquirer leaseAcquirer,
        BlobVerifier blobVerifier,
        BlobContentExtractor blobContentExtractor,
        ServiceConfiguration serviceConfiguration
    ) {
        this.dispatcher = dispatcher;
        this.envelopeService = envelopeService;
        this.leaseAcquirer = leaseAcquirer;
        this.blobVerifier = blobVerifier;
        this.blobContentExtractor = blobContentExtractor;
        this.storageConfig = serviceConfiguration.getStorageConfig();
    }

    public void process(BlobClient blobClient) {
        logger.info("Processing {} from {} container", blobClient.getBlobName(), blobClient.getContainerName());
        process(
            blobClient,
            () -> envelopeService.createNewEnvelope(
                blobClient.getContainerName(),
                blobClient.getBlobName(),
                blobClient.getProperties().getLastModified().toInstant()
            )
        );
    }

    private void process(BlobClient blobClient, Supplier<UUID> envelopeIdSupplier) {
        leaseAcquirer
            .acquireFor(blobClient)
            .ifPresentOrElse(
                leaseClient -> {
                    UUID id = envelopeIdSupplier.get();
                    try {
                        byte[] rawBlob = downloadBlob(blobClient);

                        var verificationResult = blobVerifier.verifyZip(blobClient.getBlobName(), rawBlob);

                        if (verificationResult.isOk) {
                            dispatch(blobClient, id, rawBlob);
                        } else {
                            reject(blobClient, id, verificationResult.error);
                        }
                    } catch (Exception exception) {
                        handleError(id, blobClient, exception);
                    } finally {
                        tryToReleaseLease(leaseClient, blobClient.getBlobName(), blobClient.getContainerName());
                    }
                },
                () -> logger.info(
                    "Cannot acquire a lease for blob - skipping. File name: {}, container: {}",
                    blobClient.getBlobName(),
                    blobClient.getContainerName()
                )
            );
    }

    private void dispatch(BlobClient blob, UUID id, byte[] rawBlob) throws IOException {
        StorageConfigItem containerConfig = storageConfig.get(blob.getContainerName());
        TargetStorageAccount targetStorageAccount = containerConfig.getTargetStorageAccount();

        dispatcher.dispatch(
            blob.getBlobName(),
            blobContentExtractor.getContentToUpload(rawBlob, targetStorageAccount),
            containerConfig.getTargetContainer(),
            targetStorageAccount
        );

        envelopeService.markAsDispatched(id);

        logger.info(
            "Finished processing {} from {} container. New envelope ID: {}",
            blob.getBlobName(),
            blob.getContainerName(),
            id
        );
    }

    private void reject(BlobClient blob, UUID id, String reason) {
        envelopeService.markAsRejected(id, reason);

        logger.error(
            "Rejected Blob. File name: {}, Container: {}, New envelope ID: {}, Reason: {}",
            blob.getBlobName(),
            blob.getContainerName(),
            id,
            reason
        );
        // TODO send notification to Exela
    }

    private byte[] downloadBlob(BlobClient blobClient) throws IOException {
        try (var outputStream = new ByteArrayOutputStream()) {
            blobClient.download(outputStream);

            return outputStream.toByteArray();
        } catch (BlobStorageException exc) {
            String errorMessage = exc.getStatusCode() == BAD_GATEWAY.value()
                ? "Failed to download blob. It looks like antivirus software may be blocking the file."
                : MESSAGE_FAILED_TO_DOWNLOAD_BLOB;

            throw new ZipFileLoadException(errorMessage, exc);
        } catch (Exception exc) {
            throw new ZipFileLoadException(MESSAGE_FAILED_TO_DOWNLOAD_BLOB, exc);
        }
    }

    private void tryToReleaseLease(BlobLeaseClient leaseClient, String blobName, String containerName) {
        try {
            leaseClient.releaseLease();
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

    private void handleError(UUID envelopeId, BlobClient blob, Exception exc) {
        logger.error(
            "Error occurred while processing blob. File name: {}, Container: {}, Envelope ID: {}",
            blob.getBlobName(),
            blob.getContainerName(),
            envelopeId,
            exc
        );
        envelopeService.saveEvent(envelopeId, EventType.ERROR);
    }
}
