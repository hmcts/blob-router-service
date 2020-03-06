package uk.gov.hmcts.reform.blobrouter.services.storage;

import com.azure.core.http.HttpClient;
import com.azure.storage.blob.BlobContainerClient;
import com.azure.storage.blob.BlobContainerClientBuilder;
import com.azure.storage.common.Utility;
import com.azure.storage.common.implementation.Constants;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.reform.blobrouter.clients.bulkscanprocessor.BulkScanProcessorClient;
import uk.gov.hmcts.reform.blobrouter.clients.bulkscanprocessor.SasTokenResponse;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class BulkScanClientCacheTest {

    private BulkScanClientCache bulkScanClientCache;

    @Mock
    private BulkScanProcessorClient bulkScanProcessorClient;

    private BlobContainerClientBuilder blobContainerClientBuilder = new BlobContainerClientBuilder()
        .httpClient(HttpClient.createDefault())
        .endpoint("https://example.com");

    private Long refreshSasBeforeExpiry = 30L;

    @BeforeEach
    private void setUp() {
        this.bulkScanClientCache = new BulkScanClientCache(
            bulkScanProcessorClient,
            blobContainerClientBuilder,
            refreshSasBeforeExpiry
        );
    }

    @Test
    void should_create_bulk_scan_storage() {
        String containerName = "container123";
        var sasTokenResponse =
            new SasTokenResponse("sig=examplesign%3D&se=2020-03-05T14%3A54%3A20Z&sv=2019-02-02&sp=wl&sr=c");

        given(bulkScanProcessorClient.getSasToken(containerName)).willReturn(sasTokenResponse);

        BlobContainerClient client =
            bulkScanClientCache.getBulkScanBlobContainerClient(containerName);
        assertThat(client).isNotNull();
        verify(bulkScanProcessorClient).getSasToken(containerName);
    }

    @Test
    void should_retrieve_bulk_scan_storage_client_from_cache() {
        String containerName = "container123";

        String expiryDate = Constants.ISO_8601_UTC_DATE_FORMATTER
            .format(OffsetDateTime.now(ZoneOffset.UTC).plusSeconds(refreshSasBeforeExpiry).plusSeconds(30));

        var sasTokenResponse =
            new SasTokenResponse("sig=explesign%3D&se=" + Utility.urlEncode(expiryDate) + "&sv=2019-02-02&sp=kk&sr=k");
        given(bulkScanProcessorClient.getSasToken(containerName)).willReturn(sasTokenResponse);

        BlobContainerClient client =
            bulkScanClientCache.getBulkScanBlobContainerClient(containerName);

        assertThat(client).isNotNull();

        BlobContainerClient client2 =
            bulkScanClientCache.getBulkScanBlobContainerClient(containerName);
        assertThat(client).isSameAs(client2);
        verify(bulkScanProcessorClient,times(1)).getSasToken(containerName);
    }

    @Test
    void should_create_new_bulk_scan_storage_client_when_it_is_expired() {
        String containerName = "container123";

        String expiredDate = Constants.ISO_8601_UTC_DATE_FORMATTER
            .format(OffsetDateTime.now(ZoneOffset.UTC));

        String expiryDate = Constants.ISO_8601_UTC_DATE_FORMATTER
            .format(OffsetDateTime.now(ZoneOffset.UTC).plusSeconds(refreshSasBeforeExpiry).plusSeconds(30));

        var sasTokenResponse1 =
            new SasTokenResponse("sig=explesign%3D&se=" + Utility.urlEncode(expiredDate) + "&sv=2019-02-02&sp=kk&sr=k");
        var sasTokenResponse2 =
            new SasTokenResponse("sig=explesign%3D&se=" + Utility.urlEncode(expiryDate) + "&sv=2019-02-02&sp=kk&sr=k");

        given(bulkScanProcessorClient.getSasToken(containerName))
            .willReturn(sasTokenResponse1).willReturn(sasTokenResponse2);

        BlobContainerClient client1 =
            bulkScanClientCache.getBulkScanBlobContainerClient(containerName);

        BlobContainerClient client2 =
            bulkScanClientCache.getBulkScanBlobContainerClient(containerName);
        assertThat(client1).isNotNull();
        assertThat(client2).isNotNull();

        assertThat(client1).isNotSameAs(client2);
        verify(bulkScanProcessorClient,times(2)).getSasToken(containerName);
    }
}
