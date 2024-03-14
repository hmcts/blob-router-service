package uk.gov.hmcts.reform.blobrouter.services.storage;

import com.azure.core.util.polling.PollResponse;
import com.azure.core.util.polling.SyncPoller;
import com.azure.storage.blob.BlobServiceClient;
import com.azure.storage.blob.models.BlobCopyInfo;
import com.azure.storage.blob.sas.BlobContainerSasPermission;
import com.azure.storage.blob.sas.BlobServiceSasSignatureValues;
import com.azure.storage.blob.specialized.BlockBlobClient;
import com.google.common.io.ByteStreams;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.reform.blobrouter.exceptions.BlobStreamingException;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;

import static com.microsoft.applicationinsights.web.dependencies.apachecommons.io.FileUtils.byteCountToDisplaySize;
import static org.slf4j.LoggerFactory.getLogger;
import static uk.gov.hmcts.reform.blobrouter.services.storage.RejectedFilesHandler.REJECTED_CONTAINER_SUFFIX;

/**
 * The `BlobMover` class in Java provides methods for moving blobs between
 * containers, uploading files in chunks to Azure Blob Storage, and handling exceptions related to blob operations.
 */
@Component
public class BlobMover {

    private static final Logger logger = getLogger(BlobMover.class);
    //upload chunk size in byte in MB
    private final int uploadChunkSize;
    private final BlobServiceClient storageClient;

    public BlobMover(
        BlobServiceClient storageClient,
        @Value("${upload-chunk-size-in-bytes}") int chunkSizeInBytes
    ) {
        this.storageClient = storageClient;
        this.uploadChunkSize = chunkSizeInBytes;
    }

    /**
     * The `moveToRejectedContainer` function moves a blob from a source container to a rejected
     * container, handling copy operations and error scenarios.
     *
     * @param blobName Blob name is a unique identifier for a blob within a storage container. It is used
     *                 to reference and access the specific blob in Azure Blob Storage.
     * @param containerName The `containerName` parameter in the `moveToRejectedContainer` method represents
     *                     the name of the container where the blob is currently located. This method is
     *                      designed to move a blob from its current container to a rejected container by
     *                      creating a snapshot of the blob and then copying it to the target container.
     */
    public void moveToRejectedContainer(String blobName, String containerName) {

        BlockBlobClient sourceBlob = getBlobClient(containerName, blobName);
        BlockBlobClient targetBlob = getBlobClient(containerName + REJECTED_CONTAINER_SUFFIX, blobName);

        String loggingContext = String.format(
            "File name: %s. Source Container: %s. Target Container: %s",
            blobName,
            sourceBlob.getContainerName(),
            targetBlob.getContainerName()
        );

        if (!sourceBlob.exists()) {
            logger.error("File already deleted. {}", loggingContext);
        } else {
            String sasToken = sourceBlob
                .generateSas(
                    new BlobServiceSasSignatureValues(
                        OffsetDateTime.of(LocalDateTime.now().plus(5, ChronoUnit.MINUTES), ZoneOffset.UTC),
                        new BlobContainerSasPermission().setReadPermission(true)
                    )
                );

            if (targetBlob.exists()) {
                targetBlob.createSnapshot();
            }
            SyncPoller<BlobCopyInfo, Void> poller = null;

            try {
                poller = targetBlob
                    .beginCopy(
                        sourceBlob.getBlobUrl() + "?" + sasToken,
                        null,
                        null,
                        null,
                        null,
                        null,
                        Duration.ofSeconds(2)
                    );
                PollResponse<BlobCopyInfo> pollResponse = poller
                    .waitForCompletion(Duration.ofMinutes(5));
                logger.info(
                    "Moved to rejected  container  done from {}, Poll response: {}, Copy status: {}",
                    sourceBlob.getBlobUrl(),
                    pollResponse.getStatus(),
                    pollResponse.getValue().getCopyStatus()
                );
                sourceBlob.delete();
                logger.info("File successfully moved to rejected container. {}", loggingContext);
            } catch (Exception ex) {
                logger.error(
                    "Copy Error to rejected container, for {}",
                    sourceBlob.getBlobUrl(),
                    ex
                );

                if (poller != null) {
                    try {
                        targetBlob.abortCopyFromUrl(poller.poll().getValue().getCopyId());
                    } catch (Exception exc) {
                        logger.error(
                            "Abort Copy From Url got Error,  for {}  to rejected container",
                            sourceBlob.getBlobUrl(),
                            exc
                        );
                    }
                }
                throw ex;
            }
        }
    }

    /**
     * This Java function uploads a file in chunks to a block blob storage and handles exceptions by clearing uncommitted
     * blocks if needed.
     *
     * @param blockBlobClient The `blockBlobClient` parameter in the `uploadWithChunks` method is an
     *                        instance of `BlockBlobClient` class, which is used to interact with a block
     *                        blob in Azure Blob Storage. It provides methods for uploading data in chunks
     *                        and committing the blocks to the blob.
     * @param inStream The `inStream` parameter in the `uploadWithChunks` method is an `InputStream` that
     *                 represents the data source from which the file content will be read and uploaded in
     *                 chunks to the specified `BlockBlobClient`. This input stream allows the method to
     *                 read the file content in smaller chunks, stage and commit them appropriately.
     * @return The method `uploadWithChunks` returns a `List<String>` containing the block IDs of the
     *      uploaded chunks.
     */
    public List<String> uploadWithChunks(BlockBlobClient blockBlobClient, InputStream inStream) {
        byte[] envelopeData = new byte[uploadChunkSize];
        int blockNumber = 0;
        List<String> blockList = new ArrayList<>();
        long totalSize = 0L;
        try {
            while (inStream.available() != 0) {
                blockNumber++;
                String base64BlockId = Base64.getEncoder()
                    .encodeToString(String.format("%07d", blockNumber).getBytes());
                int numBytesRead = inStream.readNBytes(envelopeData, 0, uploadChunkSize);
                totalSize += numBytesRead;
                InputStream limitedStream;
                limitedStream = ByteStreams
                    .limit(new ByteArrayInputStream(envelopeData), numBytesRead);

                blockBlobClient
                    .stageBlock(base64BlockId, limitedStream, numBytesRead);
                blockList.add(base64BlockId);
            }
            blockBlobClient.commitBlockList(blockList);
            logger.info(
                "Upload committed  to {}, num of  block {}, total size {}",
                blockBlobClient.getBlobUrl(),
                blockList.size(),
                byteCountToDisplaySize(totalSize)
            );
        } catch (Exception ex) {
            logger.info("Upload  to {}. FAILED", blockBlobClient.getBlobUrl());
            //try to clear uncommitted blocks
            tryToDelete(blockBlobClient);
            throw new BlobStreamingException("Upload by chunk got error", ex);
        }

        return blockList;
    }

    /**
     * The function `tryToDelete` attempts to delete a block blob client and logs
     * an error message if the deletion fails.
     *
     * @param blockBlobClient The `blockBlobClient` parameter is an instance of `BlockBlobClient` class,
     *                       which represents a client to interact with a block blob in Azure Storage.
     *                        In the provided code snippet, the `tryToDelete` method attempts to delete
     *                        the block blob using the `delete` method of the `BlockBlobClient`.
     */
    private void tryToDelete(BlockBlobClient blockBlobClient) {
        try {
            blockBlobClient.delete();
        } catch (Exception exc) {
            logger.error(
                "Deleting uncommitted blocks from {} failed",
                blockBlobClient.getBlobUrl(),
                exc
            );
        }
    }

    /**
     * The function `getBlobClient` returns a BlockBlobClient for a specified blob within a given container.
     *
     * @param containerName The `containerName` parameter is the name of the container where the blob is stored.
     * @param blobName The `blobName` parameter refers to the name of the blob that you want to retrieve from
     *                 the specified blob container. It is used to uniquely identify the blob within the container.
     * @return A `BlockBlobClient` object is being returned.
     */
    private BlockBlobClient getBlobClient(String containerName, String blobName) {
        return storageClient.getBlobContainerClient(containerName).getBlobClient(blobName).getBlockBlobClient();
    }
}
