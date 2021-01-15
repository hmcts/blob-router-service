package uk.gov.hmcts.reform.blobrouter.services.storage;

import com.azure.storage.blob.BlobClient;
import com.azure.storage.blob.models.BlobErrorCode;
import com.azure.storage.blob.models.BlobStorageException;
import com.azure.storage.blob.specialized.BlobLeaseClient;
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

    private final LeaseClientProvider leaseClientProvider;
    private final BlobMetaDataHandler blobMetaDataHandler;

    public LeaseAcquirer(LeaseClientProvider leaseClientProvider, BlobMetaDataHandler blobMetaDataHandler) {
        this.leaseClientProvider = leaseClientProvider;
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
        try {
            var leaseClient = leaseClientProvider.get(blobClient);
            var leaseId = leaseClient.acquireLease(LEASE_DURATION_IN_SECONDS);

            boolean isReady = false;

            try {
                isReady = blobMetaDataHandler.isBlobReadyToUse(blobClient, leaseId);
            } catch (Exception ex) {
                logger.warn(
                    "Could not check meta data for lease expiration on file {} in container {}",
                    blobClient.getBlobName(),
                    blobClient.getContainerName()
                );
            }
            release(leaseClient, blobClient);

            if (isReady) {
                onSuccess.accept(leaseId);
                if (releaseLease) {
                    clearMetadataAndReleaseLease(leaseClient, blobClient);
                }
            } else {
                //it means lease did not acquired let the failure function decide
                onFailure.accept(LEASE_ALREADY_PRESENT);
            }


        } catch (BlobStorageException exc) {
            if (exc.getErrorCode() != LEASE_ALREADY_PRESENT && exc.getErrorCode() != BLOB_NOT_FOUND) {
                logger.error(
                    "Error acquiring lease for blob. File name: {}, Container: {}",
                    blobClient.getBlobName(),
                    blobClient.getContainerName(),
                    exc
                );
            }

            onFailure.accept(exc.getErrorCode());
        }
    }

    private void clearMetadataAndReleaseLease(
        BlobLeaseClient leaseClient,
        BlobClient blobClient
    ) {
        try {
            blobMetaDataHandler.clearAllMetaData(blobClient);
        } catch (BlobStorageException exc) {
            logger.warn(
                "Could not clear metadata, lease with ID {}. Blob: {}, container: {}",
                leaseClient.getLeaseId(),
                blobClient.getBlobName(),
                blobClient.getContainerName(),
                exc
            );
        }
    }

    private void release(BlobLeaseClient leaseClient, BlobClient blobClient) {
        try {
            leaseClient.releaseLease();
        } catch (BlobStorageException exc) {
            logger.warn(
                "Could not release the lease with ID {}. Blob: {}, container: {}",
                leaseClient.getLeaseId(),
                blobClient.getBlobName(),
                blobClient.getContainerName(),
                exc
            );
        }
    }
}
