package uk.gov.hmcts.reform.blobrouter.config;

import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;

/**
 * The `TestConfiguration` class in Java defines instance variables for storing configuration values loaded using
 * Typesafe Config library.
 */
public class TestConfiguration {

    public final String blobRouterUrl;

    public final String reconciliationApiKey;

    public final String sourceStorageAccountName;
    public final String sourceStorageAccountKey;
    public final String sourceStorageAccountUrl;
    public final String crimeSourceContainer;
    public final String pcqSourceContainer;

    /**
     * This code snippet is defining a constructor for the `TestConfiguration` class in Java. The constructor
     * initializes the instance variables of the class by loading a configuration using Typesafe Config library and
     * retrieving specific values from that configuration.
     */
    public TestConfiguration() {
        Config config = ConfigFactory.load();

        this.blobRouterUrl = config.getString("blob-router-url");
        this.reconciliationApiKey = config.getString("reconciliation-api-key");
        this.sourceStorageAccountName = config.getString("source-storage-account-name");
        this.sourceStorageAccountKey = config.getString("source-storage-account-key");
        this.sourceStorageAccountUrl = config.getString("source-storage-account-url");
        this.crimeSourceContainer = config.getString("crime-source-container");
        this.pcqSourceContainer = config.getString("pcq-source-container");
    }
}
