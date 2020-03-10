package uk.gov.hmcts.reform.blobrouter.services.storage;

import com.azure.storage.blob.BlobClient;
import com.azure.storage.blob.specialized.BlobLeaseClient;
import org.springframework.stereotype.Component;

@Component
public class LeaseAcquirer {

    private final LeaseClientProvider leaseClientProvider;

    public LeaseAcquirer(LeaseClientProvider leaseClientProvider) {
        this.leaseClientProvider = leaseClientProvider;
    }

    public BlobLeaseClient acquireFor(BlobClient blobClient) {
        var leaseClient = leaseClientProvider.get(blobClient);
        try {
            leaseClient.acquireLease(60);
            return leaseClient;
        } catch ()
    }
}
