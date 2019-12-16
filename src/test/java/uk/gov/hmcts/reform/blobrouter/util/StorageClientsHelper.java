package uk.gov.hmcts.reform.blobrouter.util;

import com.azure.core.http.HttpClient;
import com.azure.core.test.InterceptorManager;
import com.azure.core.test.TestMode;
import com.azure.core.util.Configuration;
import com.azure.storage.blob.BlobAsyncClient;
import com.azure.storage.blob.BlobContainerAsyncClient;
import com.azure.storage.blob.BlobServiceAsyncClient;
import com.azure.storage.blob.BlobServiceClientBuilder;
import com.azure.storage.blob.specialized.BlobLeaseAsyncClient;
import com.azure.storage.blob.specialized.BlobLeaseClientBuilder;
import com.azure.storage.common.StorageSharedKeyCredential;

public final class StorageClientsHelper {

    private static final StorageSharedKeyCredential STORAGE_CREDENTIALS = new StorageSharedKeyCredential(
        "testAccountName", "dGVzdGtleQ=="
    );

    private static final HttpClient HTTP_CLIENT = new StorageStubHttpClient();

    private static BlobServiceClientBuilder STORAGE_CLIENT_BUILDER = new BlobServiceClientBuilder();

    private static BlobLeaseClientBuilder LEASE_CLIENT_BUILDER = new BlobLeaseClientBuilder();

    private StorageClientsHelper() {
        // utility class construct
    }

    public static void setAzureTestMode() {
        Configuration.getGlobalConfiguration().put("AZURE_TEST_MODE", TestMode.RECORD.name());
    }

    public static BlobServiceAsyncClient getStorageClient(InterceptorManager interceptorManager) {
        return STORAGE_CLIENT_BUILDER
            .credential(STORAGE_CREDENTIALS)
            .endpoint("http://httpbin.org")
            .addPolicy(interceptorManager.getRecordPolicy())
            .httpClient(HTTP_CLIENT)
            .buildAsyncClient();
    }

    public static BlobContainerAsyncClient getContainerClient(
        InterceptorManager interceptorManager,
        String containerName
    ) {
        return getStorageClient(interceptorManager).getBlobContainerAsyncClient(containerName);
    }

    public static BlobAsyncClient getBlobClient(
        InterceptorManager interceptorManager,
        String containerName,
        String blobName
    ) {
        return getContainerClient(interceptorManager, containerName).getBlobAsyncClient(blobName);
    }

    public static BlobLeaseAsyncClient getLeaseClient(BlobAsyncClient blobClient) {
        return LEASE_CLIENT_BUILDER.blobAsyncClient(blobClient).buildAsyncClient();
    }
}
