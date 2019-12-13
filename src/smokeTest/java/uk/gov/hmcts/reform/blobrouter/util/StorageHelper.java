package uk.gov.hmcts.reform.blobrouter.util;

import com.azure.storage.blob.BlobAsyncClient;
import com.azure.storage.blob.BlobContainerAsyncClient;
import com.azure.storage.blob.BlobServiceAsyncClient;
import com.azure.storage.blob.BlobServiceClientBuilder;
import com.azure.storage.common.StorageSharedKeyCredential;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;

import java.util.function.Function;

public final class StorageHelper {

    public static final Config CONFIG = ConfigFactory.load();
    private static final String ACCOUNT_NAME = CONFIG.getString("test-storage-account-name");
    private static final String ACCOUNT_KEY = CONFIG.getString("test-storage-account-key");
    public static final String ACCOUNT_URL = CONFIG.getString("test-storage-account-url");
    public static final String CONTAINER_NAME = CONFIG.getString("test-storage-container-name");

    private static final StorageSharedKeyCredential STORAGE_CREDENTIALS = new StorageSharedKeyCredential(
        ACCOUNT_NAME,
        ACCOUNT_KEY
    );

    private static final BlobServiceClientBuilder STORAGE_CLIENT_BUILDER = new BlobServiceClientBuilder();
    public static final BlobServiceAsyncClient STORAGE_CLIENT = STORAGE_CLIENT_BUILDER
        .credential(STORAGE_CREDENTIALS)
        .endpoint(ACCOUNT_URL)
        .buildAsyncClient();

    public static final BlobContainerAsyncClient CONTAINER_CLIENT = STORAGE_CLIENT
        .getBlobContainerAsyncClient(CONTAINER_NAME);

    public static final Function<String, BlobAsyncClient> GET_BLOB_CLIENT = CONTAINER_CLIENT::getBlobAsyncClient;

    private StorageHelper() {
        // utility class constructor
    }
}
