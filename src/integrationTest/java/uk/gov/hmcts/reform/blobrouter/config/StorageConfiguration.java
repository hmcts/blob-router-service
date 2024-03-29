package uk.gov.hmcts.reform.blobrouter.config;

import com.azure.storage.blob.BlobContainerClient;
import com.azure.storage.blob.BlobServiceClient;
import com.azure.storage.blob.BlobServiceClientBuilder;
import com.azure.storage.common.StorageSharedKeyCredential;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import static org.mockito.Mockito.mock;

/**
 * The `StorageConfiguration` class in Java provides configuration for creating storage credentials and
 * clients for Azure Blob Storage, including methods for creating a `StorageSharedKeyCredential`
 * object and a `BlobServiceClient` object for local testing.
 */
@Configuration
public class StorageConfiguration {

    /**
     * The function creates and returns a StorageSharedKeyCredential object using the provided storage account name and
     * key.
     *
     * @param accountName The `accountName` parameter typically refers to the name of the storage account in Azure Blob
     *      Storage or a similar cloud storage service. This is a unique name that identifies your storage account and
     *      is used to access and manage the resources within that account.
     * @param accountKey The `accountKey` parameter is a security key that is used to authenticate and authorize access
     *      to a storage account in Azure. It is a base64-encoded string that serves as a secret key for accessing the
     *      storage account resources. It is important to keep this key secure and not expose it publicly.
     * @return A `StorageSharedKeyCredential` object is being returned, which is created using the `accountName` and
     *      `accountKey` values provided as parameters.
     */
    @Bean
    public StorageSharedKeyCredential getStorageSharedKeyCredential(
        @Value("${storage.account-name}") String accountName,
        @Value("${storage.account-key}") String accountKey
    ) {
        return new StorageSharedKeyCredential(accountName, accountKey);
    }

    /**
     * This function creates and returns a BlobServiceClient using the provided StorageSharedKeyCredential for
     * authentication.
     *
     * @param credentials The `credentials` parameter in the `getStorageClient` method is of type
     *      `StorageSharedKeyCredential`. This type of credential is used for authenticating requests to Azure Storage
     *      services using an account name and account key.
     *      It provides a way to securely access your storage account.
     * @return A BlobServiceClient object is being returned.
     */
    @Bean()
    public BlobServiceClient getStorageClient(StorageSharedKeyCredential credentials) {
        // not used as needs test context so it can actually be build
        return new BlobServiceClientBuilder()
            .endpoint("http://localhost")
            .credential(credentials)
            .buildClient();
    }

    /**
     * The function returns a mocked BlobContainerClient bean with the name "crime-storage-client".
     *
     * @return A mock BlobContainerClient object is being returned.
     */
    @Bean("crime-storage-client")
    public BlobContainerClient getCrimeContainerClient() {
        return mock(BlobContainerClient.class);
    }
}
