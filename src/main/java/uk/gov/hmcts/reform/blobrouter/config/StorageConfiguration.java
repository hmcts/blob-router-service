package uk.gov.hmcts.reform.blobrouter.config;

import com.azure.storage.blob.BlobContainerClient;
import com.azure.storage.blob.BlobContainerClientBuilder;
import com.azure.storage.blob.BlobServiceClient;
import com.azure.storage.blob.BlobServiceClientBuilder;
import com.azure.storage.blob.specialized.BlobLeaseClientBuilder;
import com.azure.storage.common.StorageSharedKeyCredential;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import uk.gov.hmcts.reform.blobrouter.services.storage.LeaseClientProvider;

@Profile({"!integration-test"})
@Configuration
public class StorageConfiguration {

    @Bean
    public StorageSharedKeyCredential getStorageSharedKeyCredential(
        @Value("${storage.account-name}") String accountName,
        @Value("${storage.account-key}") String accountKey
    ) {
        System.out.println("Source storage account name: " + accountName);
        System.out.println("Source storage account key: " + accountKey);
        return new StorageSharedKeyCredential(accountName, accountKey);
    }

    @Bean
    public LeaseClientProvider getLeaseClientProvider() {
        return blobClient -> new BlobLeaseClientBuilder().blobClient(blobClient).buildClient();
    }

    @Bean("storage-client")
    public BlobServiceClient getStorageClient(
        StorageSharedKeyCredential credentials,
        @Value("${storage.url}") String storageUrl
    ) {
        System.out.println("Source storage account name: " + credentials.getAccountName());
        System.out.println("Source storage account url: " + storageUrl);

        return new BlobServiceClientBuilder()
            .credential(credentials)
            .endpoint(storageUrl)
            .buildClient();
    }

    @Bean("crime-storage-client")
    public static BlobContainerClient getCrimeStorageClient(
        @Value("${storage.crime.connection-string}") String connectionString,
        @Value("${CRIME_DESTINATION_CONTAINER}") String containerName
    ) {
        System.out.println("Crime connection string: " + connectionString);
        return new BlobContainerClientBuilder()
            .connectionString(connectionString)
            .containerName(containerName)
            .buildClient();
    }
}
