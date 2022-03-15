package uk.gov.hmcts.reform.blobrouter.config;

import com.azure.storage.blob.BlobContainerClient;
import com.azure.storage.blob.BlobServiceClient;
import com.azure.storage.blob.BlobServiceClientBuilder;
import com.azure.storage.common.StorageSharedKeyCredential;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import static org.mockito.Mockito.mock;

@Configuration
public class StorageConfiguration {

    @Bean
    public StorageSharedKeyCredential getStorageSharedKeyCredential(
    ) {
        return new StorageSharedKeyCredential("bulkscan", "testkey");
    }

    // not used as needs test context so it can actually be build
    @Bean()
    public BlobServiceClient getStorageClient(StorageSharedKeyCredential credentials) {
        return new BlobServiceClientBuilder()
            .endpoint("http://localhost")
            .credential(credentials)
            .buildClient();
    }

    @Bean("crime-storage-client")
    public BlobContainerClient getCrimeContainerClient() {
        return mock(BlobContainerClient.class);
    }

}
