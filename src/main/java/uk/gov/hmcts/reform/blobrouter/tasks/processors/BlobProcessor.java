package uk.gov.hmcts.reform.blobrouter.tasks.processors;

import com.azure.storage.blob.BlobClient;
import com.azure.storage.blob.models.BlobStorageException;
import org.slf4j.Logger;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.reform.blobrouter.config.ServiceConfiguration;
import uk.gov.hmcts.reform.blobrouter.config.StorageConfigItem;
import uk.gov.hmcts.reform.blobrouter.config.TargetStorageAccount;
import uk.gov.hmcts.reform.blobrouter.data.events.ErrorCode;
import uk.gov.hmcts.reform.blobrouter.data.events.EventType;
import uk.gov.hmcts.reform.blobrouter.exceptions.ZipFileLoadException;
import uk.gov.hmcts.reform.blobrouter.services.BlobVerifier;
import uk.gov.hmcts.reform.blobrouter.services.EnvelopeService;
import uk.gov.hmcts.reform.blobrouter.services.storage.BlobDispatcher;

import java.io.ByteArrayOutputStream;
import java.util.Map;
import java.util.UUID;
import java.util.function.Supplier;

import static org.apache.commons.lang3.StringEscapeUtils.escapeHtml4;
import static org.slf4j.LoggerFactory.getLogger;
import static org.springframework.http.HttpStatus.BAD_GATEWAY;
import static uk.gov.hmcts.reform.blobrouter.config.TargetStorageAccount.CRIME;
import static uk.gov.hmcts.reform.blobrouter.config.TargetStorageAccount.PCQ;

@Component
@EnableConfigurationProperties(ServiceConfiguration.class)
public class BlobProcessor {

    private static final Logger logger = getLogger(BlobProcessor.class);

    private final BlobDispatcher dispatcher;
    private final EnvelopeService envelopeService;
    private final BlobVerifier blobVerifier;
    private final Map<String, StorageConfigItem> storageConfig; // container-specific configuration, by container name

    public BlobProcessor(
        BlobDispatcher dispatcher,
        EnvelopeService envelopeService,
        BlobVerifier blobVerifier,
        ServiceConfiguration serviceConfiguration
    ) {
        this.dispatcher = dispatcher;
        this.envelopeService = envelopeService;
        this.blobVerifier = blobVerifier;
        this.storageConfig = serviceConfiguration.getStorageConfig();
    }

    public void process(BlobClient blobClient) {
        logger.info("Processing {} from {} container", blobClient.getBlobName(), blobClient.getContainerName());
        handle(
            blobClient,
            () -> envelopeService.createNewEnvelope(
                blobClient.getContainerName(),
                blobClient.getBlobName(),
                blobClient.getProperties().getLastModified().toInstant()
            )
        );
    }

    public void continueProcessing(UUID envelopeId, BlobClient blob) {
        logger.info(
            "Continuing processing envelope. Envelope ID: {}, file name: {}. container: {}",
            envelopeId,
            blob.getBlobName(),
            blob.getContainerName()
        );

        handle(
            blob,
            () -> envelopeId
        );
    }

    private void handle(
        BlobClient blobClient,
        Supplier<UUID> envelopeIdSupplier
    ) {
        UUID id = envelopeIdSupplier.get();
        try {
            var verificationResult = blobVerifier.verifyZip(blobClient.getBlobName(), blobClient.openInputStream());

            if (verificationResult.isOk) {
                dispatch(blobClient, id);
            } else {
                reject(blobClient, id, verificationResult.error, verificationResult.errorDescription);
            }
        } catch (Exception exception) {
            handleError(id, blobClient, exception);
        }
    }

    private void dispatch(BlobClient blob, UUID id) {
        StorageConfigItem containerConfig = storageConfig.get(blob.getContainerName());
        TargetStorageAccount targetStorageAccount = containerConfig.getTargetStorageAccount();

        if (targetStorageAccount == CRIME || targetStorageAccount == PCQ) {
            dispatcher.dispatch(
                blob,
                containerConfig.getTargetContainer(),
                targetStorageAccount
            );
        } else {
            dispatcher.moveBlob(
                blob,
                containerConfig.getTargetContainer(),
                targetStorageAccount
            );
        }

        envelopeService.markAsDispatched(id);

        logger.info(
            "Finished processing {} from {} container. New envelope ID: {}",
            blob.getBlobName(),
            blob.getContainerName(),
            id
        );
    }

    private void reject(BlobClient blob, UUID id, ErrorCode error, String errorDescription) {
        envelopeService.markAsRejected(id, error, errorDescription);

        logger.error(
            "Rejected Blob. File name: {}, Container: {}, New envelope ID: {}, Reason: {}",
            blob.getBlobName(),
            blob.getContainerName(),
            id,
            errorDescription
        );
    }

    private byte[] downloadBlob(BlobClient blobClient) {
        try (var outputStream = new ByteArrayOutputStream()) {
            blobClient.download(outputStream);

            return outputStream.toByteArray();
        } catch (BlobStorageException exc) {
            String errorMessage = exc.getStatusCode() == BAD_GATEWAY.value()
                ? ErrorMessages.DOWNLOAD_ERROR_BAD_GATEWAY
                : ErrorMessages.DOWNLOAD_ERROR_GENERIC;

            throw new ZipFileLoadException(errorMessage, exc);
        } catch (Exception exc) {
            throw new ZipFileLoadException(ErrorMessages.DOWNLOAD_ERROR_GENERIC, exc);
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
        envelopeService.saveEvent(envelopeId, EventType.ERROR, escapeHtml4(exc.getMessage()));
    }

    public static class ErrorMessages {

        public static final String DOWNLOAD_ERROR_GENERIC =
            "Failed to download blob";

        public static final String DOWNLOAD_ERROR_BAD_GATEWAY =
            "Failed to download blob. It looks like antivirus software may be blocking the file.";
    }
}
