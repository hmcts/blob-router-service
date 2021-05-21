package uk.gov.hmcts.reform.blobrouter.tasks.processors;

import com.azure.storage.blob.BlobClient;
import org.slf4j.Logger;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.reform.blobrouter.config.ServiceConfiguration;
import uk.gov.hmcts.reform.blobrouter.config.StorageConfigItem;
import uk.gov.hmcts.reform.blobrouter.config.TargetStorageAccount;
import uk.gov.hmcts.reform.blobrouter.data.envelopes.Status;
import uk.gov.hmcts.reform.blobrouter.data.events.ErrorCode;
import uk.gov.hmcts.reform.blobrouter.data.events.EventType;
import uk.gov.hmcts.reform.blobrouter.services.BlobVerifier;
import uk.gov.hmcts.reform.blobrouter.services.EnvelopeService;
import uk.gov.hmcts.reform.blobrouter.services.storage.BlobDispatcher;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Supplier;

import static org.apache.commons.lang3.StringEscapeUtils.escapeHtml4;
import static org.slf4j.LoggerFactory.getLogger;

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

        getEnvelopeSupplier(blobClient)
            .ifPresent(envelopeSupplier -> handle(blobClient, envelopeSupplier));

    }

    private Optional<Supplier<UUID>> getEnvelopeSupplier(BlobClient blobClient) {
        var envelopeOptional =
            envelopeService.findLastEnvelope(blobClient.getBlobName(), blobClient.getContainerName());

        Supplier<UUID> envelopeSupplier = null;
        if (envelopeOptional.isPresent()) {
            var envelope = envelopeOptional.get();
            if (envelope.status != Status.CREATED) {
                logger.info("Envelope processed while getting lock {} ", envelope.getBasicInfo());
                return Optional.empty();
            } else {
                envelopeSupplier = () -> envelope.id;
            }
        } else {
            envelopeSupplier = () -> envelopeService.createNewEnvelope(
                blobClient.getContainerName(),
                blobClient.getBlobName(),
                blobClient.getProperties().getCreationTime().toInstant()
            );
        }
        return Optional.of(envelopeSupplier);
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

        dispatcher.dispatch(
            blob,
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
}
