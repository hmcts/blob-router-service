package uk.gov.hmcts.reform.blobrouter.services.storage;

import com.azure.storage.common.implementation.Constants;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.Expiry;
import org.checkerframework.checker.index.qual.NonNegative;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.blobrouter.clients.bulkscanprocessor.BulkScanProcessorClient;
import uk.gov.hmcts.reform.blobrouter.exceptions.InvalidSasTokenException;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.temporal.TemporalAccessor;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static com.azure.storage.common.implementation.Constants.UrlConstants.SAS_EXPIRY_TIME;
import static com.azure.storage.common.implementation.StorageImplUtils.parseQueryString;
import static java.time.temporal.ChronoField.INSTANT_SECONDS;
import static org.slf4j.LoggerFactory.getLogger;

@Service
public class SasTokenCache {

    private static final Logger logger = getLogger(SasTokenCache.class);
    private final BulkScanProcessorClient bulkScanSasTokenClient;
    private final long refreshSasBeforeExpiry;

    //key= container name, value = sastoken
    private static Cache<String, String> tokenCache;

    public SasTokenCache(
        BulkScanProcessorClient bulkScanSasTokenClient,
        @Value("${bulk-scan-cache.refresh-before-expire-in-sec}") long refreshSasBeforeExpiry
    ) {
        this.bulkScanSasTokenClient = bulkScanSasTokenClient;
        this.refreshSasBeforeExpiry = refreshSasBeforeExpiry;
        tokenCache = Caffeine.newBuilder()
            .expireAfter(new BulkScanSasTokenCacheExpiry())
            .build();
    }

    public String getSasToken(String containerName) {
        logger.info("Getting sas token for Container: {}", containerName);

        final String sasToken = tokenCache.get(containerName, this::createSasToken);

        logger.info("Finished getting sas token for Container: {}", containerName);

        return sasToken;
    }

    public void removeFromCache(String containerName) {
        logger.info("Invalidating cache for Container: {}", containerName);

        tokenCache.invalidate(containerName);

        logger.info("Finished invalidating cache for Container: {}", containerName);
    }

    private String createSasToken(String containerName) {
        logger.info("Making sas token call for Container: {}", containerName);

        final String sasToken = bulkScanSasTokenClient.getSasToken(containerName).sasToken;

        logger.info("Finished making sas token call for Container: {}", containerName);

        return sasToken;
    }

    private class BulkScanSasTokenCacheExpiry implements Expiry<String, String> {

        public static final String MESSAGE = "Invalid SAS, the SAS expiration time parameter not found.";

        @Override
        public long expireAfterCreate(
            @NonNull String containerName,
            @NonNull String sasToken,
            long currentTime
        ) {
            Map<String, String> map = parseQueryString(sasToken);
            return calculateTimeToExpire(
                Constants.ISO_8601_UTC_DATE_FORMATTER.parse(
                    map.computeIfAbsent(
                        SAS_EXPIRY_TIME, key -> {
                            throw new InvalidSasTokenException(MESSAGE);
                        }
                    )
                )
            );
        }

        @Override
        public long expireAfterUpdate(
            @NonNull String containerName,
            @NonNull String sasToken,
            long currentTime,
            @NonNegative long currentDuration
        ) {
            return expireAfterCreate(containerName, sasToken, currentTime);
        }

        @Override
        public long expireAfterRead(
            @NonNull String containerName,
            @NonNull String sasToken,
            long currentTime,
            @NonNegative long currentDuration
        ) {
            return currentDuration;
        }

        /**
         * calculates the remaining time to expire
         * Do not wait for the full time for expiry if remaining time is less
         * or equal to refreshSasBeforeExpiry it means cached value is expired.
         * calculation:
         * expirytime - (currenttime + refreshSasBeforeExpiry) = remaining time to expire in nano seconds
         *
         * @param expiry expiry time for sas token
         * @return remaining time to expire in nano secs,
         */
        private long calculateTimeToExpire(TemporalAccessor expiry) {
            return
                TimeUnit.NANOSECONDS.convert(
                    expiry.getLong(INSTANT_SECONDS)
                        - (OffsetDateTime.now(ZoneOffset.UTC).getLong(INSTANT_SECONDS) + refreshSasBeforeExpiry),
                    TimeUnit.SECONDS
                );
        }
    }
}
