package uk.gov.hmcts.reform.blobrouter.services.storage;

import com.azure.storage.blob.BlobClient;
import com.azure.storage.blob.BlobServiceClient;
import com.azure.storage.blob.sas.BlobContainerSasPermission;
import com.azure.storage.blob.sas.BlobServiceSasSignatureValues;
import org.slf4j.Logger;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;

import static org.slf4j.LoggerFactory.getLogger;
import static uk.gov.hmcts.reform.blobrouter.services.storage.RejectedFilesHandler.REJECTED_CONTAINER_SUFFIX;

//@Component
public class BlobMover {

    private static final Logger logger = getLogger(BlobMover.class);

    private final BlobServiceClient storageClient;

    public BlobMover(BlobServiceClient storageClient) {
        this.storageClient = storageClient;
    }

    public void moveToRejectedContainer(String blobName, String containerName) {

        BlobClient sourceBlob = getBlobClient(containerName, blobName);
        BlobClient targetBlob = getBlobClient(containerName + REJECTED_CONTAINER_SUFFIX, blobName);

        if (targetBlob.exists()) {
            targetBlob.createSnapshot();
        }

        String loggingContext = String.format(
            "File name: %s. Source Container: %s. Target Container: %s",
            blobName,
            sourceBlob.getContainerName(),
            targetBlob.getContainerName()
        );

        if (!sourceBlob.exists()) {
            logger.error("File already deleted. " + loggingContext);
        } else {
            copy(targetBlob, sourceBlob, loggingContext);
            sourceBlob.delete();
            logger.info("File successfully moved to rejected container. " + loggingContext);
        }
    }

    private void copy(BlobClient targetBlob, BlobClient sourceBlob, String loggingContext) {
        String sasToken = sourceBlob
            .generateSas(
                new BlobServiceSasSignatureValues(
                    OffsetDateTime.of(LocalDateTime.now().plus(5, ChronoUnit.MINUTES), ZoneOffset.UTC),
                    new BlobContainerSasPermission().setReadPermission(true)
                )
            );
        targetBlob
            .getBlockBlobClient()
            .copyFromUrl(sourceBlob.getBlockBlobClient().getBlobUrl() + "?" + sasToken);

        logger.info("File successfully uploaded to rejected container. " + loggingContext);
    }

    private BlobClient getBlobClient(String containerName, String blobName) {
        return storageClient.getBlobContainerClient(containerName).getBlobClient(blobName);
    }
}
