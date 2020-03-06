package uk.gov.hmcts.reform.blobrouter.services.storage;

import com.azure.storage.blob.BlobContainerClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.reform.blobrouter.clients.bulkscanprocessor.BulkScanProcessorClient;
import uk.gov.hmcts.reform.blobrouter.config.TargetStorageAccount;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
public class BlobContainerClientProviderTest {

    @Mock
    BlobContainerClient crimeClient;

    @Mock
    BulkScanProcessorClient bulkScanProcessorClient;

    @Mock
    BulkScanClientCache bulkScanClientCache;

    @Mock
    BlobContainerClient bulkScanClient;

    private BlobContainerClientProvider blobContainerClientProvider;

    @BeforeEach
    private void setUp() {
        this.blobContainerClientProvider = new BlobContainerClientProvider(
            crimeClient,
            bulkScanClientCache
        );
    }

    @Test
    void should_return_crime_client_for_crime_storage() {
        BlobContainerClient client = blobContainerClientProvider.get(TargetStorageAccount.CRIME, "crime");

        assertThat(client).isSameAs(crimeClient);
    }

    @Test
    void should_return_bulk_scan_client_for_bulk_scan_storage() {
        given(bulkScanClientCache.getBulkScanBlobContainerClient(any())).willReturn(bulkScanClient);
        BlobContainerClient client = blobContainerClientProvider.get(TargetStorageAccount.BULKSCAN, "cmc");

        assertThat(client).isNotNull();
        assertThat(client).isSameAs(bulkScanClient);

    }
}
