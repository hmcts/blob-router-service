package uk.gov.hmcts.reform.blobrouter.services.storage;

import com.azure.core.util.Context;
import com.azure.storage.blob.BlobClient;
import com.azure.storage.blob.models.BlobProperties;
import com.azure.storage.blob.models.BlobRequestConditions;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.Map;

import static org.slf4j.LoggerFactory.getLogger;
import static uk.gov.hmcts.reform.blobrouter.util.TimeZones.EUROPE_LONDON_ZONE_ID;

@Component
public class BlobMetaDataHandler {

    private static final Logger logger = getLogger(BlobMetaDataHandler.class);

    public static final String LEASE_EXPIRATION_TIME = "leaseExpirationTime";

    private final int leaseTimeout;

    public BlobMetaDataHandler(@Value("${storage-blob-lease-timeout-in-minutes}") int leaseTimeout) {
        this.leaseTimeout = leaseTimeout;
    }

    public boolean isBlobReadyToUse(BlobClient blobClient) {
        BlobProperties blobProperties = blobClient.getProperties();
        String etag = blobProperties.getETag();
        Map<String, String> blobMetaData = blobProperties.getMetadata();
        String leaseExpirationTime = blobMetaData.get(LEASE_EXPIRATION_TIME);
        var zipFilename = blobClient.getBlobName();
        var containerName = blobClient.getContainerName();

        logger.info(
            "Checking if lease acquired on file {} in container {}. Lease Expiration Time: {}",
            zipFilename,
            containerName,
            leaseExpirationTime
        );

        if (isMetaDataLeaseNotAcquiredOrExpired(leaseExpirationTime)) {
            blobMetaData.put(
                LEASE_EXPIRATION_TIME,
                LocalDateTime.now(EUROPE_LONDON_ZONE_ID)
                    .plusMinutes(leaseTimeout).toString()
            );
            blobClient.setMetadataWithResponse(
                blobMetaData,
                new BlobRequestConditions().setIfMatch("\"" + etag + "\""),
                null,
                Context.NONE
            );
            return true;
        } else {
            logger.info(
                "Lease already acquired on file {} in container {}, it will expire at {} .",
                zipFilename,
                containerName,
                blobMetaData.get(LEASE_EXPIRATION_TIME)
            );
            return false;
        }
    }

    private boolean isMetaDataLeaseNotAcquiredOrExpired(String leaseExpirationTime) {
        if (StringUtils.isBlank(leaseExpirationTime)) {
            return true; // lease not acquired on file
        } else {
            LocalDateTime leaseExpiresAt = LocalDateTime.parse(leaseExpirationTime);
            return leaseExpiresAt
                .isBefore(LocalDateTime.now(EUROPE_LONDON_ZONE_ID)); // check if lease expired
        }
    }

    public void clearAllMetaData(BlobClient blobClient) {
        blobClient.setMetadata(null);
    }
}
