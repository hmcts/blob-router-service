package uk.gov.hmcts.reform.blobrouter.tasks.processors;

import com.azure.storage.blob.BlobClient;
import com.azure.storage.blob.BlobServiceClient;
import com.azure.storage.blob.models.BlobStorageException;
import com.azure.storage.blob.specialized.BlobLeaseClient;
import org.slf4j.Logger;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.reform.blobrouter.config.ServiceConfiguration;
import uk.gov.hmcts.reform.blobrouter.config.StorageConfigItem;
import uk.gov.hmcts.reform.blobrouter.config.TargetStorageAccount;
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

    private final BlobServiceClient storageClient;
    private final BlobDispatcher dispatcher;
    private final EnvelopeService envelopeService;
    private final LeaseClientProvider leaseClientProvider;
    private final BlobVerifier blobVerifier;

    // container-specific configuration, by container name
    private final Map<String, StorageConfigItem> storageConfig;

    public BlobProcessor(
        BlobServiceClient storageClient,
        BlobDispatcher dispatcher,
        EnvelopeService envelopeService,
        LeaseClientProvider leaseClientProvider,
        BlobVerifier blobVerifier,
        ServiceConfiguration serviceConfiguration
    ) {
        this.storageClient = storageClient;
        this.dispatcher = dispatcher;
        this.envelopeService = envelopeService;
        this.leaseClientProvider = leaseClientProvider;
        this.blobVerifier = blobVerifier;
        this.storageConfig = serviceConfiguration.getStorageConfig();
    }

    public void process(String blobName, String containerName) {
        envelopeService
            .findEnvelope(blobName, containerName)
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

            leaseClient = leaseClientProvider.get(blobClient);

            leaseClient.acquireLease(60);
            byte[] rawBlob = tryToDownloadBlob(blobClient);

            var verificationResult = blobVerifier.verifyZip(blobName, rawBlob);

            Instant blobCreationDate = blobClient.getProperties().getLastModified().toInstant();

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

                UUID envelopeId = envelopeService.createDispatchedEnvelope(
                    containerName,
                    blobName,
                    blobCreationDate
                );

                logger.info(
                    "Finished processing {} from {} container. New envelope ID: {}",
                    blobName,
                    containerName,
                    envelopeId
                );
            } else {
                UUID envelopeId = envelopeService.createRejectedEnvelope(
                    containerName,
                    blobName,
                    blobCreationDate,
                    verificationResult.error
                );

                logger.error(
                    "Rejected Blob. File name: {}, Container: {}, New envelope ID: {}, Reason: {}",
                    blobName,
                    containerName,
                    envelopeId,
                    verificationResult.error
                );
                // TODO send notification to Exela
            }
        } catch (Exception exception) {
            logger.error("Error occurred while processing {} from {}", blobName, containerName, exception);
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
}
