package uk.gov.hmcts.reform.blobrouter.config;

import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;

public class TestConfiguration {

    public final String sourceStorageAccountName;
    public final String sourceStorageAccountKey;
    public final String sourceStorageAccountUrl;

    public final String bulkScanStorageConnectionString;
    public final String crimeStorageConnectionString;

    public final String crimeDestinationContainer;
    public final String crimeSourceContainer;

    public TestConfiguration() {
        Config config = ConfigFactory.load();

        this.sourceStorageAccountName = config.getString("source-storage-account-name");
        this.sourceStorageAccountKey = config.getString("source-storage-account-key");
        this.sourceStorageAccountUrl = config.getString("source-storage-account-url");

        this.bulkScanStorageConnectionString = config.getString("bulkscan-storage-connection-string");

        this.crimeStorageConnectionString = config.getString("crime-storage-connection-string");
        this.crimeDestinationContainer = config.getString("crime-source-container");
        this.crimeSourceContainer = config.getString("crime-destination-container");
    }
}
