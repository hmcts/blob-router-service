package uk.gov.hmcts.reform.blobrouter.services.storage;

import com.azure.storage.blob.BlobClient;
import com.azure.storage.blob.models.BlobErrorCode;
import com.azure.storage.blob.models.BlobStorageException;
import com.azure.storage.blob.specialized.BlobLeaseClient;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
public class LeaseAcquirer {
    public static final int LEASE_DURATION_IN_SECONDS = 60;

    private final LeaseClientProvider leaseClientProvider;

    public LeaseAcquirer(LeaseClientProvider leaseClientProvider) {
        this.leaseClientProvider = leaseClientProvider;
    }

    public Optional<BlobLeaseClient> acquireFor(BlobClient blobClient) {
        try {
            var leaseClient = leaseClientProvider.get(blobClient);
            leaseClient.acquireLease(LEASE_DURATION_IN_SECONDS);
            return Optional.of(leaseClient);
        } catch (BlobStorageException exc) {
            if (exc.getErrorCode() == BlobErrorCode.LEASE_ALREADY_PRESENT) {
                return Optional.empty();
            } else {
                throw exc;
            }
        }
    }
}
