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

    private BlockBlobClient getBlobClient(String containerName, String blobName) {
        return storageClient.getBlobContainerClient(containerName).getBlobClient(blobName).getBlockBlobClient();
    }
}
