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

/**
 * The `BlobDispatcher` class in Java contains methods to dispatch and upload files
 * to a specified destination container in a target storage account, handling inner zip content and
 * logging relevant information during the process.
 */
@Component
public class BlobDispatcher {

    private static final Logger logger = getLogger(BlobDispatcher.class);

    private final BlobContainerClientProxy blobContainerClientProxy;
    private final BlobMover blobMover;

    public BlobDispatcher(BlobContainerClientProxy blobContainerClientProxy, BlobMover blobMover) {
        this.blobContainerClientProxy = blobContainerClientProxy;
        this.blobMover = blobMover;
    }

    /**
     * The `dispatch` function logs information about dispatching a file and then uploads the file to a specified
     * destination container in a target storage account.
     *
     * @param sourceBlob The `sourceBlob` parameter is of type `BlobClient` and represents the blob that needs to be
     *      dispatched.
     * @param destinationContainer The `destinationContainer` parameter in the `dispatch` method
     *                             represents the name of the container in the target storage account
     *                             where the file from the source blob will be uploaded to. It specifies
     *                             the destination location for the file transfer operation.
     * @param targetStorageAccount The `targetStorageAccount` parameter in the `dispatch` method represents
     *                             the storage account where the file will be uploaded. It is used to
     *                             specify the destination storage account for the file transfer operation.
     */
    public void dispatch(
        BlobClient sourceBlob,
        String destinationContainer,
        TargetStorageAccount targetStorageAccount
    ) {
        logger.info(
            "Dispatching file. Blob name: {}. Container: {}. Storage: {}",
            sourceBlob.getBlobName(),
            destinationContainer,
            targetStorageAccount
        );

        uploadEnvelope(sourceBlob.getBlockBlobClient(), destinationContainer, targetStorageAccount);

        logger.info(
            "File Dispatched. Blob name: {}. Container: {}. Storage: {}",
            sourceBlob.getBlobName(),
            destinationContainer,
            targetStorageAccount
        );
    }

    /**
     * The `uploadEnvelope` function uploads the inner zip content from a source blob
     * to a specified destination container using a ZipInputStream.
     *
     * @param sourceBlob The `sourceBlob` parameter is of type `BlockBlobClient`, which represents a
     *                   client to interact with a block blob in Azure Storage. It is used to access and
     *                   manage the block blob from which the inner zip file will be uploaded.
     * @param destinationContainer The `destinationContainer` parameter in the `uploadEnvelope` method is a
     *                             String that represents the name of the container in the target storage
     *                             account where the inner zip file will be uploaded to.
     * @param targetStorageAccount The `targetStorageAccount` parameter in the `uploadEnvelope` method is
     *                             of type `TargetStorageAccount`. It is used to specify the destination
     *                             storage account where the content from the source blob will be uploaded
     *                             to. This parameter likely contains information such as the storage
     *                             account name, access key, or connection details.
     */
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

    /**
     * The `uploadContent` method processes a ZipInputStream, uploads the inner Zip content to a specified destination
     * container, and logs the upload duration.
     *
     * @param sourceBlob The `sourceBlob` parameter is of type `BlockBlobClient`
     *                   and represents the blob that contains the content to be uploaded.
     * @param destinationContainer The `destinationContainer` parameter in the `uploadContent` method
     *                             represents the name of the container in the target storage account where
     *                             the content will be uploaded. It specifies the destination container for
     *                             storing the content from the source Blob.
     * @param targetStorageAccount The `targetStorageAccount` parameter in the `uploadContent` method
     *                             represents the storage account where the content will be uploaded.
     *                             It is of type `TargetStorageAccount`, which likely contains information
     *                             such as the account name, access key, and other details required to
     *                             connect to the target storage account.
     * @param zipStream The `zipStream` parameter in the `uploadContent` method is a `ZipInputStream`
     *                  object that represents a stream of ZIP file entries. It is used to read the
     *                  contents of a ZIP file entry by entry during the upload process.
     */
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
                    "Inner Zip uploaded for blob {} to Container: {}, Upload Duration: {} sec",
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
