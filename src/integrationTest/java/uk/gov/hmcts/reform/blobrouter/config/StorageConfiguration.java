package uk.gov.hmcts.reform.blobrouter.config;

import com.azure.core.http.HttpClient;
import com.azure.storage.blob.BlobContainerClient;
import com.azure.storage.blob.BlobContainerClientBuilder;
import com.azure.storage.blob.BlobServiceClient;
import com.azure.storage.blob.BlobServiceClientBuilder;
import com.azure.storage.blob.specialized.BlobLeaseClientBuilder;
import com.azure.storage.common.StorageSharedKeyCredential;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Scope;
import uk.gov.hmcts.reform.blobrouter.services.storage.LeaseClientProvider;

import static org.mockito.Mockito.mock;
import static org.springframework.beans.factory.config.ConfigurableBeanFactory.SCOPE_PROTOTYPE;

@Configuration
public class StorageConfiguration {

    @Bean
    public StorageSharedKeyCredential getStorageSharedKeyCredential(
        @Value("${storage.account-name}") String accountName,
        @Value("${storage.account-key}") String accountKey
    ) {
        return new StorageSharedKeyCredential(accountName, accountKey);
    }

    // not used as needs test context so it can actually be build
    @Bean()
    public BlobServiceClient getStorageClient(StorageSharedKeyCredential credentials) {
        return new BlobServiceClientBuilder().credential(credentials).buildClient();
    }

    @Bean("crime-storage-client")
    public BlobContainerClient getCrimeContainerClient() {
        return mock(BlobContainerClient.class);
    }

    @Bean("bulk-scan-blob-client-builder")
    @Scope(SCOPE_PROTOTYPE)
    public BlobContainerClientBuilder getBulkScanBlobContainerClientBuilder() {
        return new BlobContainerClientBuilder()
            .httpClient(HttpClient.createDefault())
            .endpoint("https://example.com");
    }

    @Bean
    public LeaseClientProvider getLeaseClientProvider() {
        return blobClient -> new BlobLeaseClientBuilder().blobClient(blobClient).buildClient();
    }
}
