package uk.gov.hmcts.reform.blobrouter.config;

import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;

public class TestConfiguration {

    public final String blobRouterUrl;

    public final String reconciliationApiKey;

    public final String sourceStorageAccountName;
    public final String sourceStorageAccountKey;
    public final String sourceStorageAccountUrl;
    public final String crimeSourceContainer;
    public final boolean useProxyForSourceStorage;

    public TestConfiguration() {
        Config config = ConfigFactory.load();

        this.blobRouterUrl = config.getString("blob-router-url");
        this.reconciliationApiKey = config.getString("reconciliation-api-key");
        this.sourceStorageAccountName = config.getString("source-storage-account-name");
        this.sourceStorageAccountKey = config.getString("source-storage-account-key");
        this.sourceStorageAccountUrl = config.getString("source-storage-account-url");
        this.crimeSourceContainer = config.getString("crime-source-container");
        this.useProxyForSourceStorage = config.getBoolean("use-proxy-for-source-storage");
    }
}
