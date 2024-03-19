package uk.gov.hmcts.reform.blobrouter.services.storage;

import com.azure.storage.blob.BlobClient;
import com.azure.storage.blob.models.BlobErrorCode;
import com.azure.storage.blob.models.BlobStorageException;
import org.apache.http.HttpStatus;
import org.slf4j.Logger;
import org.springframework.stereotype.Component;

import java.util.function.Consumer;

import static com.azure.storage.blob.models.BlobErrorCode.BLOB_NOT_FOUND;
import static com.azure.storage.blob.models.BlobErrorCode.CONDITION_NOT_MET;
import static com.azure.storage.blob.models.BlobErrorCode.LEASE_ALREADY_PRESENT;
import static org.slf4j.LoggerFactory.getLogger;

/**
 * The `LeaseAcquirer` class in Java provides methods to acquire a lease on a blob,
 * handle success and failure scenarios, and release the lease if needed.
 */
@Component
public class LeaseAcquirer {

    private static final Logger logger = getLogger(LeaseAcquirer.class);

    private final BlobMetaDataHandler blobMetaDataHandler;

    public LeaseAcquirer(BlobMetaDataHandler blobMetaDataHandler) {
        this.blobMetaDataHandler = blobMetaDataHandler;
    }

    /**
     * The `ifAcquiredOrElse` function attempts to acquire a lease on a blob using metadata,
     * executing different actions based on success or failure.
     *
     * @param blobClient The `blobClient` parameter represents the client for interacting with a blob in a
     *                   storage service. It is used to perform operations such as checking metadata,
     *                   acquiring a lease, and releasing a lease on the blob.
     * @param onSuccess The `onSuccess` parameter in the `ifAcquiredOrElse` method is a `Runnable` that
     *                  represents the action to be executed if the blob is ready to use (i.e., if the
     *                  lease is acquired successfully). In this case, the `run()` method of the `Runnable` interface.
     * @param onFailure The `onFailure` parameter in the `ifAcquiredOrElse` method is a `Consumer`
     *                  functional interface that accepts a `BlobErrorCode` as input. This parameter
     *                  is used to handle the scenario where the blob lease is not acquired successfully.
     * @param releaseLease The `releaseLease` parameter in the `ifAcquiredOrElse` method is a boolean
     *                     flag that indicates whether the lease on the blob should be released after
     *                     the operation is completed. If `releaseLease` is set to `true`, the method will
     *                     clear the metadata and release the lease.
     */
    public void ifAcquiredOrElse(
        BlobClient blobClient,
        Runnable onSuccess,
        Consumer<BlobErrorCode> onFailure,
        boolean releaseLease
    ) {
        BlobErrorCode errorCode = LEASE_ALREADY_PRESENT;
        try {
            boolean isReady = false;
            try {
                isReady = blobMetaDataHandler.isBlobReadyToUse(blobClient);
            } catch (Exception ex) {
                if (ex instanceof BlobStorageException) {
                    errorCode = getErrorCode(blobClient, (BlobStorageException) ex);
                }

                if (errorCode == CONDITION_NOT_MET) {
                    var blobStorageException = (BlobStorageException) ex;
                    logger.info(
                        "Blob already leased for {}, Error message:  {}, Status code: {}",
                        blobClient.getBlobUrl(),
                        blobStorageException.getMessage(),
                        blobStorageException.getStatusCode()
                    );
                } else {
                    logger.error(
                        "Could not check meta data for lease expiration on file {} in container {}",
                        blobClient.getBlobName(),
                        blobClient.getContainerName(),
                        ex
                    );
                }
            }

            if (isReady) {
                onSuccess.run();
                if (releaseLease) {
                    clearMetadataAndReleaseLease(blobClient);
                }
            } else {
                //it means lease did not acquired let the failure function decide
                onFailure.accept(errorCode);
            }

        } catch (BlobStorageException exc) {
            logger.error(
                "Error acquiring lease for blob. File name: {}, Container: {}",
                blobClient.getBlobName(),
                blobClient.getContainerName(),
                exc
            );
        }
    }

    /**
     * This function retrieves the error code from a BlobStorageException, handling cases where
     * the error code is null or the status code is not found.
     *
     * @param blobClient BlobClient is an object representing a client to interact with a blob in a blob
     *                   storage service. It likely contains information such as the blob name, container
     *                   name, and methods to perform operations on the blob.
     * @param exc BlobStorageException: This is an exception that is thrown when an error occurs while
     *            interacting with Azure Blob Storage.
     * @return The method `getErrorCode` returns the BlobErrorCode associated with the BlobStorageException.
     *      If the error code is null, it logs a message with the file name and container name, and if the
     *      status code of the exception is SC_NOT_FOUND, it sets the error code to BLOB_NOT_FOUND before
     *      returning it.
     */
    private BlobErrorCode getErrorCode(BlobClient blobClient, BlobStorageException exc) {
        // sometimes there is no error code in blob storage devmode
        BlobErrorCode errorCode = exc.getErrorCode();
        if (errorCode == null) {
            logger.info("Error code is NULL, File name: {}, Container: {}",
                blobClient.getBlobName(),
                blobClient.getContainerName(),
                exc
            );
            if (exc.getStatusCode() == HttpStatus.SC_NOT_FOUND) {
                errorCode = BLOB_NOT_FOUND;
            }
        }
        return errorCode;
    }

    /**
     * This function clears all metadata from a blob using a BlobClient and logs a warning if an exception occurs.
     *
     * @param blobClient BlobClient is an object representing a client to interact with a specific blob in Azure Blob
     *      Storage. It contains information about the blob such as its name and the container it belongs to.
     */
    private void clearMetadataAndReleaseLease(
        BlobClient blobClient
    ) {
        try {
            blobMetaDataHandler.clearAllMetaData(blobClient);
        } catch (BlobStorageException exc) {
            logger.warn(
                "Could not clear metadata, Blob: {}, container: {}",
                blobClient.getBlobName(),
                blobClient.getContainerName(),
                exc
            );
        }
    }
}
