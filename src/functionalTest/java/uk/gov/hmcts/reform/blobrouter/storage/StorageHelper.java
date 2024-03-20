package uk.gov.hmcts.reform.blobrouter.storage;

import com.azure.storage.blob.BlobServiceClient;

import java.io.ByteArrayInputStream;

/**
 * The `StorageHelper` class provides methods for uploading files to Azure Blob Storage and checking the existence of
 * blobs within a container.
 */
public final class StorageHelper {

    private StorageHelper() {
        // utility class
    }

    /**
     * The `uploadFile` function uploads a file to a specified container in a Blob storage service using the
     * Azure Storage SDK for Java.
     *
     * @param client The `BlobServiceClient` object represents a client to interact with Azure Blob storage. It provides
     *      methods to perform operations like creating containers, uploading and downloading blobs, etc.
     * @param containerName The `containerName` parameter refers to the name of the container in the Azure Blob Storage
     *      where you want to upload the file. It is used to identify the specific container within the Blob storage
     *      account.
     * @param fileName The `fileName` parameter in the `uploadFile` method represents the name of the file that you want
     *      to upload to the Azure Blob Storage. It is a string value that specifies the name under which the file will
     *      be stored in the Blob Storage container.
     * @param fileContent The `fileContent` parameter in the `uploadFile` method is a byte array that represents the
     *      content of the file that you want to upload to the Azure Blob Storage. It contains the actual data of the
     *      file in binary format.
     */
    public static void uploadFile(
        BlobServiceClient client,
        String containerName,
        String fileName,
        byte[] fileContent
    ) {
        client
            .getBlobContainerClient(containerName)
            .getBlobClient(fileName)
            .getBlockBlobClient()
            .upload(new ByteArrayInputStream(fileContent), fileContent.length, false);
    }

    /**
     * The function `blobExists` checks if a blob with a specific file name exists in a given container within a Blob
     *      storage account.
     *
     * @param client The `BlobServiceClient` object represents a client to interact with Azure Blob storage. It is used
     *      to perform operations on blob containers and blobs within those containers.
     * @param containerName The `containerName` parameter refers to the name of the container where the blob is stored
     *      in the Azure Blob Storage account.
     * @param fileName The `fileName` parameter in the `blobExists` method is the name of the blob file that you want to
     *      check for existence within a specific blob container.
     * @return The method `blobExists` returns a boolean value indicating whether a blob with the specified `fileName`
     *      exists in the blob container specified by `containerName` within the `BlobServiceClient` instance `client`.
     */
    public static boolean blobExists(
        BlobServiceClient client,
        String containerName,
        String fileName
    ) {
        return client
            .getBlobContainerClient(containerName)
            .getBlobClient(fileName)
            .getBlockBlobClient()
            .exists();
    }
}
