package uk.gov.hmcts.reform.blobrouter.tasks.processors;

import com.azure.core.http.rest.PagedIterable;
import com.azure.storage.blob.BlobContainerClient;
import com.azure.storage.blob.BlobServiceClient;
import com.azure.storage.blob.models.BlobItem;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import uk.gov.hmcts.reform.blobrouter.services.BlobVerifier;
import uk.gov.hmcts.reform.blobrouter.services.storage.LeaseAcquirer;

import java.util.Iterator;

import static java.util.Collections.emptyList;
import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;

class TeamContainerProcessorTest {

    @Mock
    BlobServiceClient storageClient;
    @Mock
    LeaseAcquirer leaseAcquirer;
    @Mock
    BlobVerifier blobVerifier;

    TeamContainerProcessor teamContainerProcessor;

    @BeforeEach
    void setUp() {
        storageClient = Mockito.mock(BlobServiceClient.class);
        teamContainerProcessor = new TeamContainerProcessor(storageClient, leaseAcquirer, blobVerifier);
    }

    @Test
    @SuppressWarnings("unchecked")
    void should_return_an_empty_list() {
        var blobContainerClient = Mockito.mock(BlobContainerClient.class);
        given(storageClient.getBlobContainerClient(anyString()))
            .willReturn(blobContainerClient);
        given(blobContainerClient.generateSas(any()))
            .willReturn("testSas");

        Iterator<BlobItem> mockIterator = (Iterator<BlobItem>) Mockito.mock(Iterator.class);
        given(mockIterator.hasNext()).willReturn(true, false);
        given(mockIterator.next()).willReturn(Mockito.mock(BlobItem.class));

        PagedIterable<BlobItem> mockPagedBlobItems = (PagedIterable<BlobItem>) Mockito.mock(PagedIterable.class);

        given(mockPagedBlobItems.iterator()).willReturn(mockIterator);

        given(blobContainerClient.listBlobs())
            .willReturn(mockPagedBlobItems);

        var envelopes = teamContainerProcessor.leaseAndGetEnvelopes("nfd");

        assertThat(envelopes).isEqualTo(emptyList());
    }
}
