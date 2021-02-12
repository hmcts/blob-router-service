package uk.gov.hmcts.reform.blobrouter.tasks.processors;

import com.azure.storage.blob.BlobClient;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.reform.blobrouter.config.ServiceConfiguration;
import uk.gov.hmcts.reform.blobrouter.config.StorageConfigItem;
import uk.gov.hmcts.reform.blobrouter.config.TargetStorageAccount;
import uk.gov.hmcts.reform.blobrouter.data.events.ErrorCode;
import uk.gov.hmcts.reform.blobrouter.data.events.EventType;
import uk.gov.hmcts.reform.blobrouter.services.BlobVerifier;
import uk.gov.hmcts.reform.blobrouter.services.EnvelopeService;
import uk.gov.hmcts.reform.blobrouter.services.storage.BlobDispatcher;

import java.util.Map;
import java.util.UUID;
import java.util.function.Supplier;

import static org.apache.commons.lang3.StringEscapeUtils.escapeHtml4;
import static org.slf4j.LoggerFactory.getLogger;
import static uk.gov.hmcts.reform.blobrouter.config.TargetStorageAccount.CFT;
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
    private final boolean extractEnvelopeForCft;

    public BlobProcessor(
        BlobDispatcher dispatcher,
        EnvelopeService envelopeService,
        BlobVerifier blobVerifier,
        ServiceConfiguration serviceConfiguration,
        @Value("${extract-envelope-for-cft}") boolean extractEnvelopeForCft
    ) {
        this.dispatcher = dispatcher;
        this.envelopeService = envelopeService;
        this.blobVerifier = blobVerifier;
        this.storageConfig = serviceConfiguration.getStorageConfig();
        this.extractEnvelopeForCft = extractEnvelopeForCft;
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

        if ((targetStorageAccount == CRIME || targetStorageAccount == PCQ)
            || (targetStorageAccount == CFT && extractEnvelopeForCft)) {
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
