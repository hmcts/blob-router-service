package uk.gov.hmcts.reform.blobrouter.services.storage;

import com.azure.core.exception.HttpResponseException;
import com.azure.storage.blob.BlobContainerClient;
import com.azure.storage.blob.specialized.BlockBlobClient;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.blobrouter.config.TargetStorageAccount;

import java.util.function.Consumer;

import static org.slf4j.LoggerFactory.getLogger;

/**
 * The `BlobContainerClientProxy` class in Java provides methods to interact with different types of blob containers in
 * various storage accounts, including uploading blobs and handling exceptions.
 */
@Service
public class BlobContainerClientProxy {

    private static final Logger logger = getLogger(BlobContainerClientProxy.class);

    private final BlobContainerClient crimeClient;
    private final BlobContainerClientBuilderProvider blobContainerClientBuilderProvider;
    private final SasTokenCache sasTokenCache;

    public BlobContainerClientProxy(
        @Qualifier("crime-storage-client") BlobContainerClient crimeClient,
        BlobContainerClientBuilderProvider blobContainerClientBuilderProvider,
        SasTokenCache sasTokenCache
    ) {
        this.crimeClient = crimeClient;
        this.blobContainerClientBuilderProvider = blobContainerClientBuilderProvider;
        this.sasTokenCache = sasTokenCache;
    }

    /**
     * The function `get` returns a `BlobContainerClient` based on the specified `TargetStorageAccount`.
     *
     * @param targetStorageAccount The `targetStorageAccount` parameter is an enum representing different
     *                             types of storage accounts. The method `get` takes this parameter along
     *                             with a `containerName` and returns a `BlobContainerClient` based on the
     *                             specified storage account type. The method uses a switch statement to
     *                             handle different cases.
     * @param containerName The `containerName` parameter is a String that represents the name of the blob
     *                      container in the storage account that you want to retrieve.
     * @return The method `get` returns a `BlobContainerClient` based on the `targetStorageAccount`
     *      parameter. The returned `BlobContainerClient` is obtained using different configurations
     *      depending on the value of `targetStorageAccount`.
     */
    private BlobContainerClient get(TargetStorageAccount targetStorageAccount, String containerName) {
        switch (targetStorageAccount) {
            case CFT -> {
                return blobContainerClientBuilderProvider
                    .getBlobContainerClientBuilder()
                    .sasToken(sasTokenCache.getSasToken(containerName))
                    .containerName(containerName)
                    .buildClient();
            }
            case CRIME -> {
                return crimeClient;
            }
            case PCQ -> {
                return blobContainerClientBuilderProvider
                    .getPcqBlobContainerClientBuilder()
                    .sasToken(sasTokenCache.getPcqSasToken(containerName))
                    .containerName(containerName)
                    .buildClient();
            }
            default ->
                throw new UnknownStorageAccountException(
                    String.format("Client requested for an unknown storage account: %s", targetStorageAccount)
            );
        }
    }

    /**
     * The `runUpload` function streams a blob from a source BlobClient to a destination container in a target storage
     * account and handles exceptions related to uploading.
     *
     * @param sourceBlob The `sourceBlob` parameter in the `runUpload` method is of type
     *                   `BlockBlobClient` and represents the blob that needs to be uploaded.
     *                   It contains information about the blob such as its name, URL, and other properties.
     * @param destinationContainer The `destinationContainer` parameter in the `runUpload` method represents
     *                             the name of the container in the target storage account where the blob
     *                             will be uploaded. It is a String value that specifies the destination
     *                             container for the upload operation.
     * @param targetStorageAccount The `targetStorageAccount` parameter in the `runUpload` method represents
     *                            the destination storage account where the blob will be uploaded. It is of
     *                             type `TargetStorageAccount`, which seems to be an enum or a custom class
     *                             that defines different types of storage accounts
     *                             (e.g., CFT, PC and so forth).
     * @param upload The `upload` parameter in the `runUpload` method is a `Consumer<BlockBlobClient>`.
     *               This means that it is a functional interface that takes an input of type
     *               `BlockBlobClient` and performs some operation without returning any result.
     */
    public void runUpload(
        BlockBlobClient sourceBlob,
        String destinationContainer,
        TargetStorageAccount targetStorageAccount,
        Consumer<BlockBlobClient> upload
    ) {
        try {
            var blobName = sourceBlob.getBlobName();
            logger.info("""
                    Start streaming from blob {} to Container: {}
                """,
                sourceBlob.getBlobUrl(),
                destinationContainer
            );

            final BlockBlobClient blockBlobClient =
                get(targetStorageAccount, destinationContainer)
                    .getBlobClient(blobName)
                    .getBlockBlobClient();
            upload.accept(blockBlobClient);
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
}
