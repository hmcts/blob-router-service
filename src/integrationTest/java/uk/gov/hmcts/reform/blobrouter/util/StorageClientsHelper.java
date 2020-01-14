package uk.gov.hmcts.reform.blobrouter.util;

import com.azure.core.http.HttpClient;
import com.azure.core.test.InterceptorManager;
import com.azure.core.test.TestMode;
import com.azure.core.util.Configuration;
import com.azure.storage.blob.BlobContainerClient;
import com.azure.storage.blob.BlobServiceClient;
import com.azure.storage.blob.BlobServiceClientBuilder;
import com.azure.storage.common.StorageSharedKeyCredential;

public final class StorageClientsHelper {

    private static final StorageSharedKeyCredential STORAGE_CREDENTIALS = new StorageSharedKeyCredential(
        "testAccountName", "dGVzdGtleQ=="
    );

    private static final HttpClient HTTP_CLIENT = new StorageStubHttpClient();

    private static BlobServiceClientBuilder STORAGE_CLIENT_BUILDER = new BlobServiceClientBuilder();

    private StorageClientsHelper() {
        // utility class construct
    }

    public static void setAzureTestMode() {
        Configuration.getGlobalConfiguration().put("AZURE_TEST_MODE", TestMode.RECORD.name());
    }

    public static BlobServiceClient getStorageClient(InterceptorManager interceptorManager) {
        return STORAGE_CLIENT_BUILDER
            .credential(STORAGE_CREDENTIALS)
            .endpoint("http://httpbin.org") // Do not change the url. This url is validated in the Azure test library
            .addPolicy(interceptorManager.getRecordPolicy())
            .httpClient(HTTP_CLIENT)
            .buildClient();
    }

    public static BlobContainerClient getContainerClient(
        InterceptorManager interceptorManager,
        String containerName
    ) {
        return getStorageClient(interceptorManager).getBlobContainerClient(containerName);
    }

}
