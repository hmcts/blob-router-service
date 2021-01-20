package uk.gov.hmcts.reform.blobrouter.services.storage;

import com.azure.storage.blob.BlobClient;
import com.azure.storage.blob.models.BlobErrorCode;
import com.azure.storage.blob.models.BlobStorageException;
import com.azure.storage.blob.specialized.BlobLeaseClient;
import org.apache.http.HttpStatus;
import org.slf4j.Logger;
import org.springframework.stereotype.Component;

import java.util.function.Consumer;

import static com.azure.storage.blob.models.BlobErrorCode.BLOB_NOT_FOUND;
import static com.azure.storage.blob.models.BlobErrorCode.LEASE_ALREADY_PRESENT;
import static org.slf4j.LoggerFactory.getLogger;

@Component
public class LeaseAcquirer {

    public static final int LEASE_DURATION_IN_SECONDS = 60;
    private static final Logger logger = getLogger(LeaseAcquirer.class);

    private final BlobMetaDataHandler blobMetaDataHandler;

    public LeaseAcquirer(BlobMetaDataHandler blobMetaDataHandler) {
        this.blobMetaDataHandler = blobMetaDataHandler;
    }

    /**
     * Main wrapper for blobs to be leased by {@link BlobLeaseClient}.
     * @param blobClient Represents blob
     * @param onSuccess Consumer which takes in {@code leaseId} acquired with {@link BlobLeaseClient}
     * @param onFailure Extra step to execute in case an error occurred
     * @param releaseLease Flag weather to release the lease or not
     */
    public void ifAcquiredOrElse(
        BlobClient blobClient,
        Consumer<String> onSuccess,
        Consumer<BlobErrorCode> onFailure,
        boolean releaseLease
    ) {
        BlobErrorCode errorCode = LEASE_ALREADY_PRESENT;
        try {
            boolean isReady = false;
            try {
                isReady = blobMetaDataHandler.isBlobReadyToUse(blobClient);
            } catch (Exception ex) {
                logger.warn(
                    "Could not check meta data for lease expiration on file {} in container {}",
                    blobClient.getBlobName(),
                    blobClient.getContainerName(),
                    ex
                );
                if (ex instanceof BlobStorageException) {
                    errorCode = getErrorCode(blobClient, (BlobStorageException) ex);
                }
            }

            if (isReady) {
                onSuccess.accept(null);
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
