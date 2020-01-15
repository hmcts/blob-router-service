package uk.gov.hmcts.reform.blobrouter.services.storage;

import com.azure.storage.blob.BlobClient;
import com.azure.storage.blob.specialized.BlobLeaseClient;

public interface LeaseClientProvider {
    BlobLeaseClient get(BlobClient blobClient);
}
