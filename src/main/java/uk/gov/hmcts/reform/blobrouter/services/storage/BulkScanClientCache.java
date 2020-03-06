package uk.gov.hmcts.reform.blobrouter.services.storage;

import com.azure.storage.blob.BlobContainerClient;
import com.azure.storage.blob.BlobContainerClientBuilder;
import com.azure.storage.common.implementation.Constants;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.reform.blobrouter.clients.bulkscanprocessor.BulkScanProcessorClient;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.temporal.TemporalAccessor;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static com.azure.storage.common.implementation.Constants.UrlConstants.SAS_EXPIRY_TIME;
import static com.azure.storage.common.implementation.StorageImplUtils.parseQueryString;
import static java.time.temporal.ChronoField.INSTANT_SECONDS;

@Component
public class BulkScanClientCache {

    private final BulkScanProcessorClient bulkScanSasTokenClient;

    private final BlobContainerClientBuilder blobContainerClientBuilder;

    private final Long refreshSasBeforeExpiry;

    private Map<String, CacheItem> cacheMap = new ConcurrentHashMap<>();

    public BulkScanClientCache(
        BulkScanProcessorClient bulkScanSasTokenClient,
        @Qualifier("bulk-scan-blob-client-builder") BlobContainerClientBuilder blobContainerClientBuilder,
        @Value("${bulk-scan.blob-client.refresh-sas-before-expire-in-sec}") Long refreshSasBeforeExpiry) {
        this.bulkScanSasTokenClient = bulkScanSasTokenClient;
        this.blobContainerClientBuilder = blobContainerClientBuilder;
        this.refreshSasBeforeExpiry = refreshSasBeforeExpiry;
    }

    public BlobContainerClient getBulkScanBlobContainerClient(String containerName) {

        CacheItem item = cacheMap.get(containerName);
        if (item != null && isSasValid(item.expiry)) {
            return item.blobContainerClient;
        } else {
            var newItem = new CacheItem(containerName, bulkScanSasTokenClient.getSasToken(containerName).sasToken);
            cacheMap.put(containerName, newItem);
            return newItem.blobContainerClient;
        }
    }

    private boolean isSasValid(TemporalAccessor expiry) {
        return expiry.getLong(INSTANT_SECONDS)
            - (OffsetDateTime.now(ZoneOffset.UTC).getLong(INSTANT_SECONDS) + refreshSasBeforeExpiry) > 0;
    }

    private class CacheItem {

        public final TemporalAccessor expiry;
        public final BlobContainerClient blobContainerClient;

        public CacheItem(String containerName, String sasToken) {
            Map<String, String> map = parseQueryString(sasToken);
            this.expiry = Constants.ISO_8601_UTC_DATE_FORMATTER.parse(map.get(SAS_EXPIRY_TIME));
            blobContainerClient = blobContainerClientBuilder
                .sasToken(sasToken)
                .containerName(containerName)
                .buildClient();
        }
    }
}
