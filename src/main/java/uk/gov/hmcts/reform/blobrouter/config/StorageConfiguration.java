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
