package uk.gov.hmcts.reform.blobrouter.services.storage;

import com.azure.storage.blob.BlobClient;
import com.azure.storage.blob.models.BlobStorageException;
import com.azure.storage.blob.specialized.BlobLeaseClient;
import org.slf4j.Logger;
import org.springframework.stereotype.Component;

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

    public void ifAcquiredOrElse(BlobClient blobClient, Runnable onSuccess, Runnable onBlobNotFound) {
        try {
            var leaseClient = leaseClientProvider.get(blobClient);
            leaseClient.acquireLease(LEASE_DURATION_IN_SECONDS);

            onSuccess.run();

            release(leaseClient, blobClient);

        } catch (BlobStorageException exc) {
            if (exc.getErrorCode() != LEASE_ALREADY_PRESENT && exc.getErrorCode() != BLOB_NOT_FOUND) {
                logger.error(
                    "Error acquiring lease for blob. File name: {}, Container: {}",
                    blobClient.getBlobName(),
                    blobClient.getContainerName(),
                    exc
                );
            }

            if (exc.getErrorCode() == BLOB_NOT_FOUND) {
                onBlobNotFound.run();
            } else {
                logger.info(
                    "Cannot acquire a lease for blob - skipping. File name: {}, container: {}",
                    blobClient.getBlobName(),
                    blobClient.getContainerName()
                );
            }
        }
    }

    public void ifAcquiredOrElse(BlobClient blobClient, Runnable onSuccess) {
        ifAcquiredOrElse(blobClient, onSuccess, () -> {});
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
