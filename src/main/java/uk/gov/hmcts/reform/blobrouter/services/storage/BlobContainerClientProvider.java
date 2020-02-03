package uk.gov.hmcts.reform.blobrouter.services.storage;

import com.azure.core.http.ProxyOptions;
import com.azure.core.http.netty.NettyAsyncHttpClientBuilder;
import com.azure.core.util.Configuration;
import com.azure.storage.blob.BlobContainerClient;
import com.azure.storage.blob.BlobContainerClientBuilder;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.blobrouter.clients.bulkscanprocessor.BulkScanProcessorClient;
import uk.gov.hmcts.reform.blobrouter.config.TargetStorageAccount;

import java.net.InetSocketAddress;

@Service
public class BlobContainerClientProvider {

    private final BlobContainerClient crimeClient;
    private final BulkScanProcessorClient bulkScanSasTokenClient;
    private final String bulkScanStorageUrl;

    public BlobContainerClientProvider(
        @Qualifier("crime-storage-client") BlobContainerClient crimeClient,
        BulkScanProcessorClient bulkScanSasTokenClient,
        @Value("${storage.bulkscan.url}") String bulkScanStorageUrl
    ) {
        this.crimeClient = crimeClient;
        this.bulkScanSasTokenClient = bulkScanSasTokenClient;
        this.bulkScanStorageUrl = bulkScanStorageUrl;
    }

    public BlobContainerClient get(TargetStorageAccount targetStorageAccount, String containerName) {
        switch (targetStorageAccount) {
            case BULKSCAN:
                Configuration config = new Configuration();
                config.put(Configuration.PROPERTY_HTTPS_PROXY, "proxyout.reform.hmcts.net:8080");
                config.put(Configuration.PROPERTY_HTTP_PROXY, "proxyout.reform.hmcts.net:8080");
                // retrieving a SAS token every time we're getting a client, but this will be cached in the future
                return new BlobContainerClientBuilder()
                    .sasToken(bulkScanSasTokenClient.getSasToken(containerName).sasToken)
                    .endpoint(bulkScanStorageUrl)
                    .configuration(config)
                    .httpClient(
                        // this client is the only client built. and is built by default
                        new NettyAsyncHttpClientBuilder()
                            .proxy(new ProxyOptions(
                                ProxyOptions.Type.HTTP,
                                InetSocketAddress.createUnresolved("proxyout.reform.hmcts.net", 8080)
                            ))
                            .build()
                    )
                    .containerName(containerName)
                    .buildClient();
            case CRIME:
                return crimeClient;
            default:
                throw new UnknownStorageAccountException(
                    String.format("Client requested for an unknown storage account: %s", targetStorageAccount)
                );
        }
    }
}
