package uk.gov.hmcts.reform.blobrouter.services;

import com.azure.storage.blob.models.BlobItem;
import org.apache.commons.lang3.Validate;

import java.time.Duration;

import static java.time.OffsetDateTime.now;

public class RejectedBlobChecker {

    private final Duration ttl;

    public RejectedBlobChecker(Duration ttl) {
        Validate.isTrue(ttl != null, "TTL is required");
        Validate.isTrue(!ttl.isNegative(), "TTL cannot be negative");
        this.ttl = ttl;
    }

    public boolean shouldBeDeleted(BlobItem blobItem) {
        return blobItem
            .getProperties()
            .getLastModified()
            .plus(ttl)
            .isBefore(now());
    }
}
