package uk.gov.hmcts.reform.blobrouter.config;

import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;

public class TestConfiguration {

    public final String blobRouterUrl;

    public final String sourceStorageAccountName;
    public final String sourceStorageAccountKey;
    public final String sourceStorageAccountUrl;

    public final String crimeStorageConnectionString;

    public final String crimeDestinationContainer;
    public final String crimeSourceContainer;

    public TestConfiguration() {
        Config config = ConfigFactory.load();

        this.blobRouterUrl = config.getString("blob-router-url");

        this.sourceStorageAccountName = config.getString("source-storage-account-name");
        this.sourceStorageAccountKey = config.getString("source-storage-account-key");
        this.sourceStorageAccountUrl = config.getString("source-storage-account-url");

        this.crimeStorageConnectionString = config.getString("crime-storage-connection-string");
        this.crimeSourceContainer = config.getString("crime-source-container");
        this.crimeDestinationContainer = config.getString("crime-destination-container");

        System.out.println("Crime storage connection string: " + crimeStorageConnectionString);
        System.out.println("Crime source container: " + crimeSourceContainer);
        System.out.println("Crime destination container: " + crimeDestinationContainer);

        System.out.println("Source account name: " + sourceStorageAccountName);
        System.out.println("Source account key: " + sourceStorageAccountKey);
        System.out.println("Source account url: " + sourceStorageAccountUrl);
    }
}
