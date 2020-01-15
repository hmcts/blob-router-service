package uk.gov.hmcts.reform.blobrouter.config;

import com.azure.storage.blob.specialized.BlobLeaseClientBuilder;
import com.azure.storage.blob.BlobServiceClient;
import com.azure.storage.blob.BlobServiceClientBuilder;
import com.azure.storage.common.StorageSharedKeyCredential;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import uk.gov.hmcts.reform.blobrouter.services.storage.LeaseClientProvider;
import org.springframework.context.annotation.Profile;

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
    public LeaseClientProvider getLeaseClientProvider() {
        return blobClient -> new BlobLeaseClientBuilder().blobClient(blobClient).buildClient();
    }

    @Profile({"!integration-test"})
    @Bean("bulkscan-storage-client")
    public static BlobServiceClient getBulkScanStorageClient(
        @Value("${storage.bulkscan.connection-string}") String connectionString
    ) {
        return new BlobServiceClientBuilder().connectionString(connectionString).buildClient();
    }

    @Profile({"!integration-test"})
    @Bean("crime-storage-client")
    public static BlobServiceClient getCrimeStorageClient(
        @Value("${storage.crime.connection-string}") String connectionString
    ) {
        return new BlobServiceClientBuilder().connectionString(connectionString).buildClient();
    }
}
