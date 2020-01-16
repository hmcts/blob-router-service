package uk.gov.hmcts.reform.blobrouter.storage;

import com.azure.storage.blob.BlobServiceClient;

import java.io.ByteArrayInputStream;

public final class StorageHelper {

    private StorageHelper() {
        // utility class
    }

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

    public static boolean blobExists(
        BlobServiceClient client,
        String containerName,
        String fileName
    ) {
        return client
            .getBlobContainerClient(containerName)
            .getBlobClient(fileName)
            .exists();
    }
}
