package uk.gov.hmcts.reform.blobrouter.services.storage;

import com.azure.storage.common.implementation.Constants;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.Expiry;
import org.checkerframework.checker.index.qual.NonNegative;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.blobrouter.clients.bulkscanprocessor.BulkScanProcessorClient;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.temporal.TemporalAccessor;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static com.azure.storage.common.implementation.Constants.UrlConstants.SAS_EXPIRY_TIME;
import static com.azure.storage.common.implementation.StorageImplUtils.parseQueryString;
import static java.time.temporal.ChronoField.INSTANT_SECONDS;

@Service
public class BulkScanContainerClientCache {

    private final BulkScanProcessorClient bulkScanSasTokenClient;
    private final Long refreshSasBeforeExpiry;

    private static Cache<String, String> tokenCache;

    public BulkScanContainerClientCache(
        BulkScanProcessorClient bulkScanSasTokenClient,
        @Value("${bulk-scan.blob-client.refresh-sas-before-expire-in-sec}") Long refreshSasBeforeExpiry) {
        this.bulkScanSasTokenClient = bulkScanSasTokenClient;
        this.refreshSasBeforeExpiry = refreshSasBeforeExpiry;
        tokenCache = Caffeine.newBuilder()
            .expireAfter(new BulkScanContainerCacheExpiry())
            .build();
    }

    public String getSasToken(String containerName) {
        return tokenCache.get(containerName, userName -> this.createSasToken(containerName));
    }

    private String createSasToken(String containerName) {
        return bulkScanSasTokenClient.getSasToken(containerName).sasToken;
    }

    private long calculateTimeToExpire(TemporalAccessor expiry) {
        return
            TimeUnit.NANOSECONDS.convert(
                expiry.getLong(INSTANT_SECONDS)
                    - (OffsetDateTime.now(ZoneOffset.UTC).getLong(INSTANT_SECONDS) + refreshSasBeforeExpiry),
                TimeUnit.SECONDS);
    }

    private class BulkScanContainerCacheExpiry implements Expiry<String, String> {

        @Override
        public long expireAfterCreate(
            @NonNull String key,
            @NonNull String sasToken, long currentTime) {
            Map<String, String> map = parseQueryString(sasToken);
            return calculateTimeToExpire(Constants.ISO_8601_UTC_DATE_FORMATTER.parse(map.get(SAS_EXPIRY_TIME)));
        }

        @Override
        public long expireAfterUpdate(
            @NonNull String key, @NonNull String value, long currentTime,
            @NonNegative long currentDuration) {
            return currentDuration;
        }

        @Override
        public long expireAfterRead(
            @NonNull String key, @NonNull String value, long currentTime,
            @NonNegative long currentDuration) {
            return currentDuration;
        }
    }
}
