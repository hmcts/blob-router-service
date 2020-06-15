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

    public LeaseAcquirer(LeaseClientProvider leaseClientProvider) {
        this.leaseClientProvider = leaseClientProvider;
    }

    /**
     * Main wrapper for blobs to be leased by {@link BlobLeaseClient} and perform deletion task.
     * @param blobClient Represents blob
     * @param onSuccess Runnable task to perform when lease is acquired
     * @param onFailure Extra step to execute in case an error occurred
     */
    public void processAndRelease(BlobClient blobClient, Runnable onSuccess, Consumer<BlobErrorCode> onFailure) {
        var leaseClient = ifAcquiredOrElse(blobClient, leaseId -> onSuccess.run(), onFailure);

        if (leaseClient != null) {
            release(leaseClient, blobClient);
        }
    }

    /**
     * Main wrapper for blobs to be leased by {@link BlobLeaseClient} and perform non-delete task.
     * @param blobClient Represents blob
     * @param onSuccess Consumer which takes in {@code leaseId} acquired with {@link BlobLeaseClient}
     * @param onFailure Extra step to execute in case an error occurred
     */
    public BlobLeaseClient ifAcquiredOrElse(
        BlobClient blobClient,
        Consumer<String> onSuccess,
        Consumer<BlobErrorCode> onFailure
    ) {
        try {
            var leaseClient = leaseClientProvider.get(blobClient);
            var leaseId = leaseClient.acquireLease(LEASE_DURATION_IN_SECONDS);

            onSuccess.accept(leaseId);

            return leaseClient;
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

            return null;
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
