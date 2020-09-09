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
import reactor.util.function.Tuple2;
import reactor.util.function.Tuples;
import uk.gov.hmcts.reform.blobrouter.clients.bulkscanprocessor.BulkScanProcessorClient;
import uk.gov.hmcts.reform.blobrouter.config.TargetStorageAccount;
import uk.gov.hmcts.reform.blobrouter.exceptions.InvalidSasTokenException;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.temporal.TemporalAccessor;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;

import static com.azure.storage.common.implementation.Constants.UrlConstants.SAS_EXPIRY_TIME;
import static com.azure.storage.common.implementation.StorageImplUtils.parseQueryString;
import static java.time.temporal.ChronoField.INSTANT_SECONDS;
import static org.slf4j.LoggerFactory.getLogger;

@Service
public class SasTokenCache {

    private static final Logger logger = getLogger(SasTokenCache.class);

    // key - storage-account name - value - Map {key= container name, value = sastoken}
    private Map<TargetStorageAccount, Tuple2<Cache<String, String>, Function<String, String>>> cacheManager;

    public SasTokenCache(
        BulkScanProcessorClient bulkScanSasTokenClient,
        @Value("${bulk-scan-cache.refresh-before-expire-in-sec}") long bulkscanSasTokenTtl
    ) {
        cacheManager = new HashMap<>();
        /* Bulkscan storage containers sas token cache config */
        addCacheConfig(
            TargetStorageAccount.BULKSCAN,
            bulkscanSasTokenTtl,
            (String container) -> bulkScanSasTokenClient.getSasToken(container).sasToken
        );

        /* TODO: Add PCQ storage container sas token cache config */
    }

    private void addCacheConfig(TargetStorageAccount storageAccount, long ttl, Function<String, String> getSasToken) {
        cacheManager.put(
            storageAccount,
            Tuples.of(
                Caffeine.newBuilder().expireAfter(new SasTokenCacheExpiry(ttl)).build(),
                getSasToken
            )
        );
    }

    public String getSasToken(TargetStorageAccount service, String containerName) {
        logger.info("Getting sas token for Container: {}", containerName);

        Tuple2<Cache<String, String>, Function<String, String>> cacheTuple = cacheManager.get(service);
        final String sasToken = cacheTuple.getT1().get(containerName, cacheTuple.getT2());

        logger.info("Finished getting sas token for Container: {}", containerName);

        return sasToken;
    }

    public void removeFromCache(TargetStorageAccount service, String containerName) {
        logger.info("Invalidating cache for Container: {}", containerName);

        Tuple2<Cache<String, String>, Function<String, String>> cacheTuple = cacheManager.get(service);
        cacheTuple.getT1().invalidate(containerName);

        logger.info("Finished invalidating cache for Container: {}", containerName);
    }

    private class SasTokenCacheExpiry implements Expiry<String, String> {

        public static final String MESSAGE = "Invalid SAS, the SAS expiration time parameter not found.";
        private long sasTokenCacheTtl;

        public SasTokenCacheExpiry(long sasTokenCacheTtl) {
            this.sasTokenCacheTtl = sasTokenCacheTtl;
        }

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
                        - (OffsetDateTime.now(ZoneOffset.UTC).getLong(INSTANT_SECONDS) + sasTokenCacheTtl),
                    TimeUnit.SECONDS
                );
        }
    }
}
