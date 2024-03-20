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

/**
 * The `BlobProcessor` class in Java processes blob objects from cloud storage, handles envelopes, verifies content,
 * dispatches or rejects based on verification results, and logs relevant information.
 */
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

    /**
     * The `process` function logs information about a BlobClient and handles it if an envelope supplier is present.
     *
     * @param blobClient BlobClient is an object representing a blob in cloud storage.
     *                   It contains information such as the blob name and the container name where the blob is stored.
     */
    public void process(BlobClient blobClient) {
        logger.info("Processing {} from {} container", blobClient.getBlobName(), blobClient.getContainerName());

        getEnvelopeSupplier(blobClient)
            .ifPresent(envelopeSupplier -> handle(blobClient, envelopeSupplier));

    }

    /**
     * This function retrieves a supplier of UUID based on the status of an envelope associated with a BlobClient.
     *
     * @param blobClient BlobClient is an object representing a blob in a storage service.
     *                   It contains information such as the blob's name, container name, properties
     *                   like creation time and size.
     * @return An Optional containing a Supplier that provides a UUID value is being returned.
     */
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
                blobClient.getProperties().getCreationTime().toInstant(),
                blobClient.getProperties().getBlobSize()
            );
        }
        return Optional.of(envelopeSupplier);
    }

    /**
     * The `handle` function processes a BlobClient using an envelope ID supplier, verifying the BlobClient content and
     * dispatching or rejecting based on the verification result.
     *
     * @param blobClient BlobClient is a class representing a client for interacting with blobs,
     *                   which are binary large objects typically used for storing data in a database or
     *                   a file system. It likely contains methods for accessing blob data and metadata.
     * @param envelopeIdSupplier The `envelopeIdSupplier` is a `Supplier` functional interface that
     *                           supplies (or generates) a UUID. In the `handle` method, it is used to get
     *                           a UUID by calling its `get()` method. This UUID is then used in the
     *                           processing logic within the method.
     */
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

    /**
     * The `dispatch` method processes a BlobClient, dispatches it to a target storage account,
     * marks it as dispatched, and logs the completion details.
     *
     * @param blob The `blob` parameter in the `dispatch` method is of type `BlobClient`, which represents a
     *             client to interact with a blob in Azure Blob Storage. It contains information about the
     *             blob such as its name, container name, and other metadata.
     * @param id The `id` parameter in the `dispatch` method is of type `UUID` and is used to uniquely
     *           identify the blob being processed. It is passed to the method to mark the blob as dispatched
     *           after processing is completed.
     */
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

    /**
     * The `reject` function marks a blob as rejected and logs relevant information.
     *
     * @param blob The `blob` parameter is of type `BlobClient`, which represents a client that interacts
     *             with a blob in Azure Blob Storage. It provides methods for working with blobs such as
     *             uploading, downloading, deleting, and listing blobs.
     * @param id The `id` parameter is a `UUID` representing the unique identifier of the blob being rejected.
     * @param error The `error` parameter in the `reject` method is of type `ErrorCode`. It is used to
     *              specify the error code associated with the rejection of a blob.
     * @param errorDescription The `errorDescription` parameter in the `reject` method is a string that
     *                         provides a description or reason for why the blob was rejected. It is used
     *                         to log the reason for rejection along with other details such as the file
     *                         name, container, and new envelope ID.
     */
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

    /**
     * This function logs an error message related to processing a blob and saves an error event with the message in an
     * envelope service.
     *
     * @param envelopeId The `envelopeId` parameter is a unique identifier (UUID) associated with an envelope.
     * @param blob The `blob` parameter in the `handleError` method represents a BlobClient object, which is
     *             used to interact with a blob (binary large object) in Azure Blob Storage. It provides
     *             methods to  manage and manipulate the blob, such as getting the blob's name (`blob.getBlobName()`).
     * @param exc The `exc` parameter in the `handleError` method is of type `Exception` and represents
     *            the exception that occurred while processing the blob.
     */
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
