package uk.gov.hmcts.reform.blobrouter.config;

import com.azure.core.http.HttpClient;
import com.azure.storage.blob.BlobContainerClient;
import com.azure.storage.blob.BlobContainerClientBuilder;
import com.azure.storage.blob.BlobServiceClient;
import com.azure.storage.blob.BlobServiceClientBuilder;
import com.azure.storage.common.StorageSharedKeyCredential;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

/**
 * The `StorageConfiguration` class in Java provides configuration beans for connecting to Azure Blob Storage, including
 * creating a BlobServiceClient and a BlobContainerClient for storing crime-related data.
 */
@Profile({"!integration-test"})
@Configuration
public class StorageConfiguration {

    @Bean
    public StorageSharedKeyCredential getStorageSharedKeyCredential(
        @Value("${storage.account-name}") String accountName,
        @Value("${storage.account-key}") String accountKey
    ) {
        return new StorageSharedKeyCredential(accountName, accountKey);
    }

    /**
     * The function creates and returns a BlobServiceClient using the provided account information and HttpClient for
     * Azure Blob Storage.
     *
     * @param accountName The `accountName` parameter is the name of the Azure Storage account that you want to connect
     *      to. It is used to identify the specific storage account within Azure.
     * @param accountKey The `accountKey` parameter in the code snippet you provided is used to specify the account key
     *      for accessing the Azure Storage account. This key is a security credential that is used to authenticate
     *      and authorize access to the storage account. It is important to keep the account key secure and not expose
     *      it publicly.
     * @param storageUrl The `storageUrl` parameter typically represents the URL endpoint for the Azure Blob Storage
     *      service. It is used to specify the location where the Blob service can be accessed.
     *      follows the format `https://{accountName}.blob.core.windows.net/`, where `{accountName}` is the name of
     * @param azureHttpClient The `azureHttpClient` parameter in the `getStorageClient` method is an instance of the
     *      `HttpClient` class provided by Azure SDK. It is used to configure the HTTP client that will be used for
     *      making requests to the Azure Blob storage service. This allows you to customize the behavior of the HTTP
     *      request.
     * @return A BlobServiceClient object is being returned.
     */
    @Bean
    public BlobServiceClient getStorageClient(
        @Value("${storage.account-name}") String accountName,
        @Value("${storage.account-key}") String accountKey,
        @Value("${storage.url}") String storageUrl,
        HttpClient azureHttpClient
    ) {
        String connectionString = String.format(
            "DefaultEndpointsProtocol=https;BlobEndpoint=%s;AccountName=%s;AccountKey=%s",
            storageUrl,
            accountName,
            accountKey
        );

        return new BlobServiceClientBuilder()
            .connectionString(connectionString)
            .httpClient(azureHttpClient)
            .buildClient();
    }

    /**
     * The function `getCrimeStorageClient` creates and returns a BlobContainerClient for accessing a storage container
     * related to crime data.
     *
     * @param connectionString The `connectionString` parameter typically contains the connection information needed to
     *      connect to a storage service, such as Azure Blob Storage. It usually includes details like the account name,
     *      account key, and endpoint URL required to establish a connection to the storage service.
     *      This information is essential for authenticating and authorizing
     * @param containerName The `containerName` parameter is the name of the container in the Azure Blob Storage where
     *      you want to store crime-related data. This parameter is typically provided as a configuration value to
     *      specify the destination container for storing crime data.
     * @return A BlobContainerClient object for accessing a storage container for storing crime-related data.
     */
    @Bean("crime-storage-client")
    public static BlobContainerClient getCrimeStorageClient(
        @Value("${storage.crime.connection-string}") String connectionString,
        @Value("${CRIME_DESTINATION_CONTAINER}") String containerName
    ) {
        return new BlobContainerClientBuilder()
            .connectionString(connectionString)
            .containerName(containerName)
            .buildClient();
    }
}
