package uk.gov.hmcts.reform.blobrouter.services;

import com.azure.storage.blob.models.BlobItem;
import org.apache.commons.lang3.Validate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.Duration;

import static java.time.OffsetDateTime.now;

@Component
public class RejectedBlobChecker {
    private static final Logger log = LoggerFactory.getLogger(RejectedBlobChecker.class);

    private final Duration ttl;

    public RejectedBlobChecker(
        @Value("${scheduling.task.delete-rejected-files.ttl}") Duration ttl
    ) {
        Validate.isTrue(ttl != null, "TTL is required");
        Validate.isTrue(!ttl.isNegative(), "TTL cannot be negative");
        this.ttl = ttl;
    }

    public boolean shouldBeDeleted(BlobItem blobItem) {
        log.info(
            "Item {} , TTL {}, should be deleted  {}",
            blobItem.getName(),
            ttl,
            blobItem
                .getProperties()
                .getCreationTime()
                .plus(ttl)
                .isBefore(now())
        );
        return blobItem
            .getProperties()
            .getCreationTime()
            .plus(ttl)
            .isBefore(now());
    }
}
