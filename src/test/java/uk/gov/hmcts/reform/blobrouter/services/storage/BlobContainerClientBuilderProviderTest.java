package uk.gov.hmcts.reform.blobrouter.services.storage;

import com.azure.core.http.HttpClient;
import com.azure.storage.blob.BlobContainerClientBuilder;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;

import static org.assertj.core.api.Assertions.assertThat;

class BlobContainerClientBuilderProviderTest {


    @Mock
    private HttpClient httpClient;

    private BlobContainerClientBuilderProvider blobContainerClientBuilderProvider
        = new BlobContainerClientBuilderProvider(httpClient, "http://example.com");

    @Test
    void getblobContainerClientBuilderProvider() {

        BlobContainerClientBuilder blobContainerClientBuilder = blobContainerClientBuilderProvider
            .getBlobContainerClientBuilder();

        assertThat(blobContainerClientBuilder).isNotNull();
    }
}
