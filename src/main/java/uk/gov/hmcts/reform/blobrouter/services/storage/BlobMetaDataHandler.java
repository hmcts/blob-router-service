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

/**
 * The `BlobMetaDataHandler` class in Java manages metadata for Azure Blob Storage, including checking lease status,
 * updating expiration time, and clearing metadata.
 */
@Component
public class BlobMetaDataHandler {

    private static final Logger logger = getLogger(BlobMetaDataHandler.class);

    public static final String LEASE_EXPIRATION_TIME = "leaseExpirationTime";

    private final int leaseTimeout;

    public BlobMetaDataHandler(@Value("${storage-blob-lease-timeout-in-minutes}") int leaseTimeout) {
        this.leaseTimeout = leaseTimeout;
    }

    /**
     * The function `isBlobReadyToUse` checks if a lease is acquired or expired on a
     * blob and updates the lease expiration time if needed.
     *
     * @param blobClient BlobClient is a class representing a client to interact with Azure Blob Storage.
     *                   It is used to perform operations on blobs such as uploading, downloading, deleting,
     *                   and getting properties of blobs. In the provided code snippet, the method
     *                   isBlobReadyToUse takes a BlobClient object as a parameter and checks that the
     *                   metadata lease is acquired and/or expired. Performs logic based on outcome.
     * @return The method `isBlobReadyToUse` returns a boolean value - `true`
     *      if the blob is ready to use (lease acquired or renewed successfully), and `false` if the
     *      lease on the blob is already acquired and has not expired yet.
     */
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

    /**
     * The function checks if a metadata lease has not been acquired or has
     * expired based on the provided lease expiration time.
     *
     * @param leaseExpirationTime leaseExpirationTime is a String representing the expiration time of a
     *                            metadata lease in the format of "yyyy-MM-ddTHH:mm:ss".
     * @return The method `isMetaDataLeaseNotAcquiredOrExpired` returns a boolean value. It returns `true`
     *      if the `leaseExpirationTime` is blank, indicating that the lease has not been acquired for the
     *      file. It returns `false` if the `leaseExpirationTime` is not blank and the lease expiration
     *      time is before the current time in the Europe/London time zone.
     */
    private boolean isMetaDataLeaseNotAcquiredOrExpired(String leaseExpirationTime) {
        if (StringUtils.isBlank(leaseExpirationTime)) {
            return true; // lease not acquired on file
        } else {
            LocalDateTime leaseExpiresAt = LocalDateTime.parse(leaseExpirationTime);
            return leaseExpiresAt
                .isBefore(LocalDateTime.now(EUROPE_LONDON_ZONE_ID)); // check if lease expired
        }
    }

    /**
     * The function `clearAllMetaData` clears all metadata associated with a BlobClient by setting it to null.
     *
     * @param blobClient BlobClient is an object representing a client for interacting with blobs in a storage service.
     *                   In this context, it seems to be used to manage metadata associated with blobs.
     *                   The method `clearAllMetaData` is designed to clear all metadata associated with the
     *                   blobClient by setting it to null.
     */
    public void clearAllMetaData(BlobClient blobClient) {
        blobClient.setMetadata(null);
    }
}
