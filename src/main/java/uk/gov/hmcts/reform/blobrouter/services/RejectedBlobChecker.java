package uk.gov.hmcts.reform.blobrouter.services;

import com.azure.storage.blob.models.BlobItem;
import org.apache.commons.lang3.Validate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.Duration;

import static java.time.OffsetDateTime.now;

/**
 * The `RejectedBlobChecker` class in Java checks if a `BlobItem` should be deleted based on its
 * creation time and a time-to-live (TTL) value.
 */
@Component
public class RejectedBlobChecker {

    private final Duration ttl;

    public RejectedBlobChecker(
        @Value("${scheduling.task.delete-rejected-files.ttl}") Duration ttl
    ) {
        Validate.isTrue(ttl != null, "TTL is required");
        Validate.isTrue(!ttl.isNegative(), "TTL cannot be negative");
        this.ttl = ttl;
    }

    /**
     * The function determines if a BlobItem should be deleted based on its creation
     * time and a time-to-live (TTL) value.
     *
     * @param blobItem BlobItem is an object representing a blob in a storage system. It likely contains
     *                 properties such as creation time, metadata, and possibly the blob data itself.
     * @return The method `shouldBeDeleted` returns a boolean value, which indicates whether the
     *      `BlobItem` should be deleted based on its creation time and the time-to-live (ttl) value.
     */
    public boolean shouldBeDeleted(BlobItem blobItem) {
        return blobItem
            .getProperties()
            .getCreationTime()
            .plus(ttl)
            .isBefore(now());
    }
}
