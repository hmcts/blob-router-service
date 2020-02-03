package uk.gov.hmcts.reform.blobrouter.tasks.processors;

import com.azure.storage.blob.BlobClient;
import com.azure.storage.blob.BlobServiceClient;
import com.azure.storage.blob.models.BlobStorageException;
import com.azure.storage.blob.specialized.BlobLeaseClient;
import org.slf4j.Logger;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.reform.blobrouter.config.ServiceConfiguration;
import uk.gov.hmcts.reform.blobrouter.config.TargetStorageAccount;
import uk.gov.hmcts.reform.blobrouter.data.EnvelopeRepository;
import uk.gov.hmcts.reform.blobrouter.data.model.NewEnvelope;
import uk.gov.hmcts.reform.blobrouter.data.model.Status;
import uk.gov.hmcts.reform.blobrouter.exceptions.InvalidZipArchiveException;
import uk.gov.hmcts.reform.blobrouter.services.BlobReadinessChecker;
import uk.gov.hmcts.reform.blobrouter.services.BlobSignatureVerifier;
import uk.gov.hmcts.reform.blobrouter.services.storage.BlobDispatcher;
import uk.gov.hmcts.reform.blobrouter.services.storage.LeaseClientProvider;

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

    // name of the zip entry (in a blob) that contains the envelope
    private static final String ENVELOPE_ENTRY_NAME = "envelope.zip";

    private final BlobServiceClient storageClient;
    private final BlobDispatcher dispatcher;
    private final BlobReadinessChecker readinessChecker;
    private final EnvelopeRepository envelopeRepository;
    private final LeaseClientProvider leaseClientProvider;
    private final BlobSignatureVerifier signatureVerifier;

    // container-specific configuration, by container name
    private final Map<String, ServiceConfiguration.StorageConfig> storageConfig;

    public BlobProcessor(
        BlobServiceClient storageClient,
        BlobDispatcher dispatcher,
        BlobReadinessChecker readinessChecker,
        EnvelopeRepository envelopeRepository,
        LeaseClientProvider leaseClientProvider,
        BlobSignatureVerifier signatureVerifier,
        ServiceConfiguration serviceConfiguration
    ) {
        this.storageClient = storageClient;
        this.dispatcher = dispatcher;
        this.readinessChecker = readinessChecker;
        this.envelopeRepository = envelopeRepository;
        this.leaseClientProvider = leaseClientProvider;
        this.signatureVerifier = signatureVerifier;
        this.storageConfig = serviceConfiguration.getStorageConfig();
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

            Instant blobCreationDate = blobClient.getProperties().getLastModified().toInstant();

            if (this.readinessChecker.isReady(blobCreationDate)) {
                leaseClient = leaseClientProvider.get(blobClient);

                leaseClient.acquireLease(60);
                byte[] rawBlob = tryToDownloadBlob(blobClient);

                // Verify Zip signature
                boolean validSignature = signatureVerifier.verifyZipSignature(blobName, rawBlob);

                if (validSignature) {
                    ServiceConfiguration.StorageConfig containerConfig = storageConfig.get(containerName);
                    TargetStorageAccount targetStorageAccount = containerConfig.getTargetStorageAccount();
                    var targetContainerName = containerConfig.getTargetContainer();

                    dispatcher.dispatch(
                        blobName,
                        getContentToUpload(rawBlob, targetStorageAccount),
                        targetContainerName,
                        targetStorageAccount
                    );

                    UUID envelopeId = envelopeRepository.insert(createNewEnvelope(
                        blobName,
                        containerName,
                        blobCreationDate,
                        Status.DISPATCHED
                    ));

                    logger.info(
                        "Finished processing {} from {} container. New envelope ID: {}",
                        blobName,
                        containerName,
                        envelopeId
                    );
                } else {
                    UUID envelopeId = envelopeRepository.insert(createNewEnvelope(
                        blobName,
                        containerName,
                        blobCreationDate,
                        Status.REJECTED
                    ));

                    logger.error(
                        "Invalid signature. Rejected Blob name {} container {} New envelope ID: {}",
                        blobName,
                        containerName,
                        envelopeId
                    );
                    // TODO send notification to Exela
                }
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
                if (ENVELOPE_ENTRY_NAME.equals(entry.getName())) {
                    return toByteArray(zipStream);
                }
            }

            throw new InvalidZipArchiveException(
                String.format("ZIP file doesn't contain the required %s entry", ENVELOPE_ENTRY_NAME)
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

    private NewEnvelope createNewEnvelope(String blobName, String containerName, Instant fileCreatedAt, Status status) {
        return new NewEnvelope(
            containerName,
            blobName,
            fileCreatedAt,
            Instant.now(),
            status
        );
    }
}
