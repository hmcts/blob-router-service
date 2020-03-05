package uk.gov.hmcts.reform.blobrouter.tasks.processors;

import com.azure.storage.blob.BlobClient;
import com.azure.storage.blob.models.BlobErrorCode;
import com.azure.storage.blob.models.BlobStorageException;
import com.azure.storage.blob.specialized.BlobLeaseClient;
import org.slf4j.Logger;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.reform.blobrouter.config.ServiceConfiguration;
import uk.gov.hmcts.reform.blobrouter.config.StorageConfigItem;
import uk.gov.hmcts.reform.blobrouter.config.TargetStorageAccount;
import uk.gov.hmcts.reform.blobrouter.data.events.EventType;
import uk.gov.hmcts.reform.blobrouter.exceptions.InvalidZipArchiveException;
import uk.gov.hmcts.reform.blobrouter.services.BlobVerifier;
import uk.gov.hmcts.reform.blobrouter.services.EnvelopeService;
import uk.gov.hmcts.reform.blobrouter.services.storage.BlobDispatcher;
import uk.gov.hmcts.reform.blobrouter.services.storage.LeaseClientProvider;
import uk.gov.hmcts.reform.blobrouter.util.zipverification.ZipVerifiers;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import static com.google.common.io.ByteStreams.toByteArray;
import static org.slf4j.LoggerFactory.getLogger;

@Component
@EnableConfigurationProperties(ServiceConfiguration.class)
public class BlobProcessor {

    private static final Logger logger = getLogger(BlobProcessor.class);

    private final BlobDispatcher dispatcher;
    private final EnvelopeService envelopeService;
    private final LeaseClientProvider leaseClientProvider;
    private final BlobVerifier blobVerifier;

    // container-specific configuration, by container name
    private final Map<String, StorageConfigItem> storageConfig;

    public BlobProcessor(
        BlobDispatcher dispatcher,
        EnvelopeService envelopeService,
        LeaseClientProvider leaseClientProvider,
        BlobVerifier blobVerifier,
        ServiceConfiguration serviceConfiguration
    ) {
        this.dispatcher = dispatcher;
        this.envelopeService = envelopeService;
        this.leaseClientProvider = leaseClientProvider;
        this.blobVerifier = blobVerifier;
        this.storageConfig = serviceConfiguration.getStorageConfig();
    }

    public void process(BlobClient blobClient) {
        String blobName = blobClient.getBlobName();
        var containerName = blobClient.getContainerName();
        Instant blobCreationDate = blobClient.getProperties().getLastModified().toInstant();

        logger.info("Processing {} from {} container", blobName, containerName);

        BlobLeaseClient leaseClient = null;
        try {
            leaseClient = leaseClientProvider.get(blobClient);
            leaseClient.acquireLease(60);

            UUID id = envelopeService.createNewEnvelope(containerName, blobName, blobCreationDate);

            byte[] rawBlob = tryToDownloadBlob(blobClient);

            var verificationResult = blobVerifier.verifyZip(blobName, rawBlob);

            if (verificationResult.isOk) {
                StorageConfigItem containerConfig = storageConfig.get(containerName);
                TargetStorageAccount targetStorageAccount = containerConfig.getTargetStorageAccount();
                var targetContainerName = containerConfig.getTargetContainer();

                dispatcher.dispatch(
                    blobName,
                    getContentToUpload(rawBlob, targetStorageAccount),
                    targetContainerName,
                    targetStorageAccount
                );

                envelopeService.markAsDispatched(id);

                logger.info(
                    "Finished processing {} from {} container. New envelope ID: {}",
                    blobName,
                    containerName,
                    id
                );
            } else {
                envelopeService.markAsRejected(id);

                logger.error(
                    "Rejected Blob. File name: {}, Container: {}, New envelope ID: {}, Reason: {}",
                    blobName,
                    containerName,
                    id,
                    verificationResult.error
                );
                // TODO send notification to Exela
            }
        } catch (BlobStorageException exc) {
            if (exc.getErrorCode() == BlobErrorCode.LEASE_ALREADY_PRESENT) {
                logger.info("Cannot acquire a lease for blob. File name: {}, container: {}", blobName, containerName);
            } else {
                handleError(exc, blobClient);
            }
        } catch (Exception exception) {
            handleError(exception, blobClient);
        } finally {
            tryToReleaseLease(leaseClient, blobName, containerName);
        }
    }

    private byte[] getContentToUpload(
        byte[] blobContent,
        TargetStorageAccount targetStorageAccount
    ) throws IOException {
        return targetStorageAccount == TargetStorageAccount.CRIME
            ? extractEnvelopeContent(blobContent)
            : blobContent;
    }

    private byte[] extractEnvelopeContent(byte[] blobContent) throws IOException {
        try (var zipStream = new ZipInputStream(new ByteArrayInputStream(blobContent))) {
            ZipEntry entry = null;

            while ((entry = zipStream.getNextEntry()) != null) {
                if (ZipVerifiers.ENVELOPE.equals(entry.getName())) {
                    return toByteArray(zipStream);
                }
            }

            throw new InvalidZipArchiveException(
                String.format("ZIP file doesn't contain the required %s entry", ZipVerifiers.ENVELOPE)
            );
        }
    }

    private byte[] tryToDownloadBlob(BlobClient blobClient) throws IOException {
        try (var outputStream = new ByteArrayOutputStream()) {
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

    private void handleError(Exception exc, BlobClient blob) {
        logger.error("Error occurred while processing {} from {}", blob.getBlobName(), blob.getContainerName(), exc);
        envelopeService.saveEvent(blob.getContainerName(), blob.getBlobName(), EventType.ERROR);
    }
}
