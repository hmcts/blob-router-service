package uk.gov.hmcts.reform.blobrouter.config;

import com.azure.core.http.HttpClient;
import com.azure.core.http.netty.NettyAsyncHttpClientBuilder;
import com.azure.storage.blob.BlobContainerClient;
import com.azure.storage.blob.BlobContainerClientBuilder;
import com.azure.storage.blob.BlobServiceClient;
import com.azure.storage.blob.BlobServiceClientBuilder;
import com.azure.storage.blob.specialized.BlobLeaseClientBuilder;
import com.azure.storage.common.StorageSharedKeyCredential;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;
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
        return new StorageSharedKeyCredential(accountName, accountKey);
    }

    @Bean
    public LeaseClientProvider getLeaseClientProvider() {
        return blobClient -> new BlobLeaseClientBuilder().blobClient(blobClient).buildClient();
    }

    @Bean
    public BlobServiceClient getStorageClient(
        @Value("${storage.account-name}") String accountName,
        @Value("${storage.account-key}") String accountKey,
        @Value("${storage.url}") String storageUrl
    ) {
        String connectionString = String.format(
            "DefaultEndpointsProtocol=https;BlobEndpoint=%s;AccountName=%s;AccountKey=%s",
            storageUrl,
            accountName,
            accountKey
        );
        reactor.netty.http.client.HttpClient baseHttpClient = reactor.netty.http.client.HttpClient
            .create()
            .tcpConfiguration(
                tcp -> tcp.bootstrap(b -> b.handler(new LoggingHandler(LogLevel.DEBUG))));

        HttpClient client = new NettyAsyncHttpClientBuilder(baseHttpClient)
            .eventLoopGroup(new NioEventLoopGroup(10))
            .disableBufferCopy(true)
            .build();
        return new BlobServiceClientBuilder()
            .connectionString(connectionString)
            .httpClient(client)
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
