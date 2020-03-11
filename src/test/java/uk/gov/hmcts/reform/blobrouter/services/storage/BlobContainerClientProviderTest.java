package uk.gov.hmcts.reform.blobrouter.services.storage;

import com.azure.storage.blob.BlobContainerClient;
import com.azure.storage.blob.BlobContainerClientBuilder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.reform.blobrouter.config.TargetStorageAccount;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
public class BlobContainerClientProviderTest {

    @Mock
    private BlobContainerClient crimeClient;

    @Mock
    private BulkScanSasTokenCache bulkScanSasTokenCache;

    @Mock
    private BlobContainerClientBuilder blobContainerClientBuilder;

    @Mock
    private BlobContainerClientBuilderProvider blobContainerClientBuilderProvider;

    private BlobContainerClientProvider blobContainerClientProvider;

    @BeforeEach
    private void setUp() {
        this.blobContainerClientProvider = new BlobContainerClientProvider(
            crimeClient,
            blobContainerClientBuilderProvider,
            bulkScanSasTokenCache
        );
    }

    @Test
    void should_return_crime_client_for_crime_storage() {
        BlobContainerClient client = blobContainerClientProvider.get(TargetStorageAccount.CRIME, "crime");

        assertThat(client).isSameAs(crimeClient);
    }

    @Test
    void should_retrieve_sas_token_for_bulk_scan_storage() {
        String containerName = "container123";
        given(bulkScanSasTokenCache.getSasToken(any())).willReturn("token1");

        given(blobContainerClientBuilderProvider.getBlobContainerClientBuilderBean())
            .willReturn(blobContainerClientBuilder);
        given(blobContainerClientBuilder.containerName(containerName)).willReturn(blobContainerClientBuilder);
        given(blobContainerClientBuilder.sasToken("token1")).willReturn(blobContainerClientBuilder);
        given(blobContainerClientBuilder.buildClient()).willReturn(mock(BlobContainerClient.class));


        BlobContainerClient client = blobContainerClientProvider.get(TargetStorageAccount.BULKSCAN, containerName);

        assertThat(client).isNotNull();
        verify(bulkScanSasTokenCache).getSasToken(containerName);
    }
}
