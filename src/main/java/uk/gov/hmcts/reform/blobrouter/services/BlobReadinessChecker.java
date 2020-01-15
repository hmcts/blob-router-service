package uk.gov.hmcts.reform.blobrouter.services;

import org.apache.commons.lang3.Validate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

@Component
public class BlobReadinessChecker {

    private final int delayInMinutes;

    public BlobReadinessChecker(
        @Value("storage-blob-processing-delay-in-minutes") int delayInMinutes
    ) {
        Validate.isTrue(delayInMinutes >= 0, "Delay cannot be a negative number");

        this.delayInMinutes = delayInMinutes;
    }

    /**
     * Checks whether blob created on given date is ready to be processed.
     */
    public boolean isReady(Instant blobCreationDate) {
        return Instant.now().isAfter(blobCreationDate.plus(delayInMinutes, ChronoUnit.MINUTES));
    }
}
