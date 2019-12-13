package uk.gov.hmcts.reform.blobrouter.util;

import com.azure.storage.blob.BlobServiceAsyncClient;
import com.azure.storage.blob.BlobServiceClientBuilder;
import com.azure.storage.common.StorageSharedKeyCredential;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;

public final class StorageHelper {

    private static final Config CONFIG = ConfigFactory.load();
    private static final String ACCOUNT_NAME = CONFIG.getString("test-storage-account-name");
    private static final String ACCOUNT_KEY = CONFIG.getString("test-storage-account-key");
    private static final String ACCOUNT_URL = CONFIG.getString("test-storage-account-url");

    private static final StorageSharedKeyCredential STORAGE_CREDENTIALS = new StorageSharedKeyCredential(
        ACCOUNT_NAME,
        ACCOUNT_KEY
    );

    private static final BlobServiceClientBuilder STORAGE_CLIENT_BUILDER = new BlobServiceClientBuilder();
    public static final BlobServiceAsyncClient STORAGE_CLIENT = STORAGE_CLIENT_BUILDER
        .credential(STORAGE_CREDENTIALS)
        .endpoint(ACCOUNT_URL)
        .buildAsyncClient();

    private StorageHelper() {
        // utility class constructor
    }
}
