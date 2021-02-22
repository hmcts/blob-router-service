package uk.gov.hmcts.reform.blobrouter.services.storage;

import com.azure.storage.blob.BlobClient;
import com.azure.storage.blob.specialized.BlockBlobClient;
import org.slf4j.Logger;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.reform.blobrouter.config.TargetStorageAccount;
import uk.gov.hmcts.reform.blobrouter.exceptions.BlobStreamingException;
import uk.gov.hmcts.reform.blobrouter.exceptions.InvalidZipArchiveException;

import java.io.IOException;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import static org.slf4j.LoggerFactory.getLogger;
import static uk.gov.hmcts.reform.blobrouter.util.zipverification.ZipVerifiers.ENVELOPE;

@Component
public class BlobDispatcher {

    private static final Logger logger = getLogger(BlobDispatcher.class);

    private final BlobContainerClientProxy blobContainerClientProxy;
    private final BlobMover blobMover;

    public BlobDispatcher(BlobContainerClientProxy blobContainerClientProxy, BlobMover blobMover) {
        this.blobContainerClientProxy = blobContainerClientProxy;
        this.blobMover = blobMover;
    }

    public void dispatch(
        BlobClient sourceBlob,
        String destinationContainer,
        TargetStorageAccount targetStorageAccount
    ) {
        logger.info(
            "Uploading file. Blob name: {}. Container: {}. Storage: {}",
            sourceBlob.getBlobName(),
            destinationContainer,
            targetStorageAccount
        );

        uploadEnvelope(sourceBlob.getBlockBlobClient(), destinationContainer, targetStorageAccount);

        logger.info(
            "Finished uploading file. Blob name: {}. Container: {}. Storage: {}",
            sourceBlob.getBlobName(),
            destinationContainer,
            targetStorageAccount
        );
    }

    private void uploadEnvelope(
        BlockBlobClient sourceBlob,
        String destinationContainer,
        TargetStorageAccount targetStorageAccount
    ) {
        logger.info(
            "Upload inner zip  from blob {} to Container: {}",
            sourceBlob.getBlobUrl(),
            destinationContainer
        );
        try (var zipStream = new ZipInputStream(sourceBlob.openInputStream());) {
            uploadContent(sourceBlob, destinationContainer, targetStorageAccount, zipStream);
        } catch (IOException ex) {
            throw new BlobStreamingException(
                "Blob upload, source blob InputStream error.", ex
            );
        }
    }

    private void uploadContent(
        BlockBlobClient sourceBlob,
        String destinationContainer,
        TargetStorageAccount targetStorageAccount,
        ZipInputStream zipStream
    ) throws IOException {

        long startTime = System.nanoTime();
        ZipEntry entry;
        while ((entry = zipStream.getNextEntry()) != null) {
            if (Objects.equals(entry.getName(), ENVELOPE)) {
                blobContainerClientProxy.runUpload(
                    sourceBlob,
                    destinationContainer,
                    targetStorageAccount,
                    target -> blobMover.uploadWithChunks(target, zipStream)
                );

                logger.info(
                    "Streaming finished for  blob {} to Container: {}, Upload Duration: {} sec",
                    sourceBlob.getBlobUrl(),
                    destinationContainer,
                    TimeUnit.NANOSECONDS.toSeconds(System.nanoTime() - startTime)
                );
                return;
            }
        }

        throw new InvalidZipArchiveException(
            String.format("ZIP file doesn't contain the required %s entry", ENVELOPE)
        );
    }

}
