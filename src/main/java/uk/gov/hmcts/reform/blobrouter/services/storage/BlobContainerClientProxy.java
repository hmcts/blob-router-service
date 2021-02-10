package uk.gov.hmcts.reform.blobrouter.services.storage;

import com.azure.core.exception.HttpResponseException;
import com.azure.core.util.polling.PollResponse;
import com.azure.core.util.polling.SyncPoller;
import com.azure.storage.blob.BlobClient;
import com.azure.storage.blob.BlobContainerClient;
import com.azure.storage.blob.models.BlobCopyInfo;
import com.azure.storage.blob.models.ParallelTransferOptions;
import com.azure.storage.blob.sas.BlobContainerSasPermission;
import com.azure.storage.blob.sas.BlobServiceSasSignatureValues;
import com.azure.storage.blob.specialized.BlockBlobClient;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.blobrouter.config.TargetStorageAccount;
import uk.gov.hmcts.reform.blobrouter.exceptions.BlobStreamingException;
import uk.gov.hmcts.reform.blobrouter.exceptions.InvalidZipArchiveException;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import static org.slf4j.LoggerFactory.getLogger;
import static uk.gov.hmcts.reform.blobrouter.util.zipverification.ZipVerifiers.ENVELOPE;

@Service
public class BlobContainerClientProxy {

    private static final Logger logger = getLogger(BlobContainerClientProxy.class);
    //buffer size in byte, 10 KB
    public static final int BUFFER_SIZE = 1024 * 10;
    // streaming block size in byte, 50 KB
    public static final long BLOCK_SIZE = 1024L * 50L;

    private final BlobContainerClient crimeClient;
    private final BlobContainerClientBuilderProvider blobContainerClientBuilderProvider;
    private final SasTokenCache sasTokenCache;
    public static final Map<String, String> META_DATA_MAP = Map.of("waitingCopy", "true");

    public BlobContainerClientProxy(
        @Qualifier("crime-storage-client") BlobContainerClient crimeClient,
        BlobContainerClientBuilderProvider blobContainerClientBuilderProvider,
        SasTokenCache sasTokenCache
    ) {
        this.crimeClient = crimeClient;
        this.blobContainerClientBuilderProvider = blobContainerClientBuilderProvider;
        this.sasTokenCache = sasTokenCache;
    }

    private BlobContainerClient get(TargetStorageAccount targetStorageAccount, String containerName) {
        switch (targetStorageAccount) {
            case CFT:
                return blobContainerClientBuilderProvider
                    .getBlobContainerClientBuilder()
                    .sasToken(sasTokenCache.getSasToken(containerName))
                    .containerName(containerName)
                    .buildClient();
            case CRIME:
                return crimeClient;
            case PCQ:
                return blobContainerClientBuilderProvider
                    .getPcqBlobContainerClientBuilder()
                    .sasToken(sasTokenCache.getPcqSasToken(containerName))
                    .containerName(containerName)
                    .buildClient();
            default:
                throw new UnknownStorageAccountException(
                    String.format("Client requested for an unknown storage account: %s", targetStorageAccount)
                );
        }
    }

    public void streamContentToDestination(
        BlobClient sourceBlob,
        String destinationContainer,
        TargetStorageAccount targetStorageAccount
    ) {
        try {
            streamContent(sourceBlob.getBlockBlobClient(), destinationContainer, targetStorageAccount);
        } catch (HttpResponseException ex) {
            logger.info(
                "Uploading failed for blob {} to Container: {},  error code: {}",
                sourceBlob.getBlobName(),
                destinationContainer,
                ex.getResponse() == null ? ex.getMessage() : ex.getResponse().getStatusCode()
            );
            if ((targetStorageAccount == TargetStorageAccount.CFT
                || targetStorageAccount == TargetStorageAccount.PCQ)
                && HttpStatus.valueOf(ex.getResponse().getStatusCode()).is4xxClientError()) {
                sasTokenCache.removeFromCache(destinationContainer);
            }
            throw ex;
        }
    }

    private void streamContent(
        BlockBlobClient sourceBlob,
        String destinationContainer,
        TargetStorageAccount targetStorageAccount
    ) {
        var blobName = sourceBlob.getBlobName();
        logger.info("Start streaming from blob {} to Container: {}", sourceBlob.getBlobUrl(), destinationContainer);
        long startTime = System.nanoTime();
        try (var zipStream = new ZipInputStream(sourceBlob.openInputStream());) {
            ZipEntry entry;

            while ((entry = zipStream.getNextEntry()) != null) {
                if (Objects.equals(entry.getName(), ENVELOPE)) {
                    final BlockBlobClient blockBlobClient =
                        get(targetStorageAccount, destinationContainer)
                            .getBlobClient(blobName)
                            .getBlockBlobClient();

                    ParallelTransferOptions parallelTransferOptions =
                        new ParallelTransferOptions()
                            .setBlockSizeLong(BLOCK_SIZE)
                            .setMaxConcurrency(8)
                            .setMaxSingleUploadSizeLong(BLOCK_SIZE);

                    try (var blobOutputStream =
                        blockBlobClient.getBlobOutputStream(
                            parallelTransferOptions, null, null, null, null)
                    ) {

                        byte[] envelopeData = new byte[BUFFER_SIZE];
                        while (zipStream.available() != 0) {
                            int numBytesRead = zipStream.readNBytes(envelopeData, 0, BUFFER_SIZE);
                            blobOutputStream.write(envelopeData, 0, numBytesRead);
                            blobOutputStream.flush();
                        }

                    } catch (IOException ex) {
                        logger.error("Streaming  got error. Stream from {} to {}",
                            sourceBlob.getBlobUrl(),
                            destinationContainer,
                            ex
                        );
                        throw new BlobStreamingException(
                            "Blob upload, destination blob outputstream error.", ex
                        );
                    }
                    logger.info(
                        "Streaming finished for  blob {} to Container: {}, Upload Duration: {} sec",
                        sourceBlob.getBlobUrl(),
                        destinationContainer,
                        TimeUnit.NANOSECONDS.toSeconds(System.nanoTime() - startTime)
                    );

                    return;
                }
            }
        } catch (IOException ex) {
            throw new BlobStreamingException(
                "Blob upload, source blob inputstream error.", ex
            );
        }

        throw new InvalidZipArchiveException(
            String.format("ZIP file doesn't contain the required %s entry", ENVELOPE)
        );
    }

    public void upload(
        String blobName,
        byte[] blobContents,
        String destinationContainer,
        TargetStorageAccount targetStorageAccount
    ) {
        long uploadStartTime = 0;
        try {
            final BlockBlobClient blockBlobClient =
                get(targetStorageAccount, destinationContainer)
                    .getBlobClient(blobName)
                    .getBlockBlobClient();

            logger.info("Uploading content of blob {} to Container: {}", blobName, destinationContainer);
            uploadStartTime = System.currentTimeMillis();
            blockBlobClient
                .upload(
                    new ByteArrayInputStream(blobContents),
                    blobContents.length
            );

            logger.info("Finished uploading content of blob {} to Container: {}", blobName, destinationContainer);
        } catch (HttpResponseException ex) {
            logger.info(
                "Uploading failed for blob {} to Container: {}, upload duration in Ms: {}, error code: {}",
                blobName,
                destinationContainer,
                (System.currentTimeMillis() - uploadStartTime),
                ex.getResponse() == null ? ex.getMessage() : ex.getResponse().getStatusCode()
            );
            if ((targetStorageAccount == TargetStorageAccount.CFT
                || targetStorageAccount == TargetStorageAccount.PCQ)
                && HttpStatus.valueOf(ex.getResponse().getStatusCode()).is4xxClientError()) {
                sasTokenCache.removeFromCache(destinationContainer);
            }
            throw ex;
        }
    }

    public void moveBlob(
        BlobClient sourceBlob,
        String destinationContainer,
        TargetStorageAccount targetStorageAccount
    ) {
        try {
            var blobName = sourceBlob.getBlobName();
            logger.info("Move from {}   to Container: {}", sourceBlob.getBlobUrl(),
                destinationContainer);

            String sasToken = sourceBlob
                .generateSas(
                    new BlobServiceSasSignatureValues(
                        OffsetDateTime
                            .of(LocalDateTime.now().plus(5, ChronoUnit.MINUTES), ZoneOffset.UTC),
                        new BlobContainerSasPermission().setReadPermission(true)
                    )
                );
            final BlockBlobClient targetBlob =
                get(targetStorageAccount, destinationContainer)
                    .getBlobClient(blobName)
                    .getBlockBlobClient();

            var start = System.nanoTime();
            SyncPoller<BlobCopyInfo, Void> poller = null;
            try {
                poller = targetBlob
                    .beginCopy(
                        sourceBlob.getBlobUrl() + "?" + sasToken,
                        META_DATA_MAP,
                        null,
                        null,
                        null,
                        null,
                        Duration.ofSeconds(2)
                    );

                PollResponse<BlobCopyInfo> pollResponse = poller
                    .waitForCompletion(Duration.ofMinutes(5));
                targetBlob.setMetadata(null);
                logger.info("Move done from {}   to Container: {} Poll response: {}, Copy status: {} ,Takes {} second",
                    sourceBlob.getBlobUrl(),
                    destinationContainer,
                    pollResponse.getStatus(),
                    pollResponse.getValue().getCopyStatus(),
                    TimeUnit.NANOSECONDS.toSeconds(System.nanoTime() - start)
                );

            } catch (Exception ex) {
                logger.error("Copy Error, for {}  to Container: {} ",
                    sourceBlob.getBlobUrl(),
                    destinationContainer,
                    ex
                );

                if (poller != null) {
                    try {
                        targetBlob.abortCopyFromUrl(poller.poll().getValue().getCopyId());
                    } catch (Exception exc) {
                        logger.error("Abort Copy From Url got Error,  for {}  to Container: {} ",
                            sourceBlob.getBlobUrl(),
                            destinationContainer,
                            exc
                        );
                    }
                }
                throw ex;
            }

        } catch (HttpResponseException ex) {
            logger.info(
                "Uploading failed for  url {} to Container: {}, error code: {}",
                sourceBlob.getBlobUrl(),
                destinationContainer,
                ex.getResponse() == null ? ex.getMessage() : ex.getResponse().getStatusCode()
            );
            if ((targetStorageAccount == TargetStorageAccount.CFT
                || targetStorageAccount == TargetStorageAccount.PCQ)
                && HttpStatus.valueOf(ex.getResponse().getStatusCode()).is4xxClientError()) {
                sasTokenCache.removeFromCache(destinationContainer);
            }
            throw ex;
        }
    }

}
