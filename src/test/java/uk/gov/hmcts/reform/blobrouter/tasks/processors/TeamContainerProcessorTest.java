package uk.gov.hmcts.reform.blobrouter.tasks.processors;

import com.azure.core.http.rest.PagedIterable;
import com.azure.storage.blob.BlobClient;
import com.azure.storage.blob.BlobContainerClient;
import com.azure.storage.blob.BlobServiceClient;
import com.azure.storage.blob.models.BlobItem;
import com.azure.storage.blob.models.BlobItemProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import uk.gov.hmcts.reform.blobrouter.services.BlobVerifier;
import uk.gov.hmcts.reform.blobrouter.services.storage.LeaseAcquirer;

import java.time.OffsetDateTime;
import java.util.ArrayList;
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
        leaseAcquirer = Mockito.mock(LeaseAcquirer.class);
        blobVerifier = Mockito.mock(BlobVerifier.class);
        teamContainerProcessor = new TeamContainerProcessor(storageClient, leaseAcquirer, blobVerifier);
    }

    @Test
    void should_return_an_empty_list_from_exception() {
        var blobContainerClient = Mockito.mock(BlobContainerClient.class);
        given(storageClient.getBlobContainerClient(anyString()))
            .willThrow(new RuntimeException());

        var envelopes = teamContainerProcessor.leaseAndGetEnvelopes("nfd");

        assertThat(envelopes).isEqualTo(emptyList());
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

    @Test
    @SuppressWarnings("unchecked")
    void should_return_single_item() {
        var blobItemName = "testBlobItem";
        var blobContainerClient = Mockito.mock(BlobContainerClient.class);
        given(storageClient.getBlobContainerClient(anyString()))
            .willReturn(blobContainerClient);
        given(blobContainerClient.generateSas(any()))
            .willReturn("testSas");
        given(blobContainerClient.getBlobClient(blobItemName))
            .willReturn(Mockito.mock(BlobClient.class));

        ArrayList<BlobItem> blobItemList = new ArrayList<>();
        BlobItem blobItem = new BlobItem();
        blobItem.setName(blobItemName);
        blobItemList.add(blobItem);

        PagedIterable<BlobItem> mockPagedBlobItems = (PagedIterable<BlobItem>) Mockito.mock(PagedIterable.class);

        given(mockPagedBlobItems.stream()).willReturn(blobItemList.stream());

        given(blobContainerClient.listBlobs())
            .willReturn(mockPagedBlobItems);

        given(blobVerifier.verifyZip(anyString(), any()))
            .willReturn(BlobVerifier.VerificationResult.OK_VERIFICATION_RESULT);

        var envelopes = teamContainerProcessor.leaseAndGetEnvelopes("nfd");

        assertThat(envelopes).isEqualTo(emptyList());
    }

    @Test
    @SuppressWarnings("unchecked")
    void should_return_single_item_with_verification_error() {
        var blobItemName = "testBlobItem";
        var blobContainerClient = Mockito.mock(BlobContainerClient.class);
        given(storageClient.getBlobContainerClient(anyString()))
            .willReturn(blobContainerClient);
        given(blobContainerClient.generateSas(any()))
            .willReturn("testSas");
        given(blobContainerClient.getBlobClient(blobItemName))
            .willReturn(Mockito.mock(BlobClient.class));

        var props = new BlobItemProperties();
        props.setCreationTime(OffsetDateTime.now())
            .setContentLength(100L)
            .setContentType("application/json");

        BlobItem blobItem = new BlobItem();
        blobItem.setName(blobItemName);
        blobItem.setProperties(props);

        ArrayList<BlobItem> blobItemList = new ArrayList<>();
        blobItemList.add(blobItem);

        PagedIterable<BlobItem> mockPagedBlobItems = (PagedIterable<BlobItem>) Mockito.mock(PagedIterable.class);

        given(mockPagedBlobItems.stream()).willReturn(blobItemList.stream());

        given(blobContainerClient.listBlobs())
            .willReturn(mockPagedBlobItems);

        given(blobVerifier.verifyZip(any(), any()))
            .willReturn(BlobVerifier.INVALID_SIGNATURE_VERIFICATION_RESULT);

        var envelopes = teamContainerProcessor.leaseAndGetEnvelopes("nfd");

        assertThat(envelopes.toArray().length).isEqualTo(1);
    }
}
