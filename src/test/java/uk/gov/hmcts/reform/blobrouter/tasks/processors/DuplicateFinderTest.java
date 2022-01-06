package uk.gov.hmcts.reform.blobrouter.tasks.processors;

import com.azure.core.http.rest.PagedIterable;
import com.azure.storage.blob.BlobContainerClient;
import com.azure.storage.blob.BlobServiceClient;
import com.azure.storage.blob.models.BlobItem;
import com.azure.storage.blob.models.BlobItemProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.reform.blobrouter.data.envelopes.Envelope;
import uk.gov.hmcts.reform.blobrouter.services.EnvelopeService;
import uk.gov.hmcts.reform.blobrouter.tasks.processors.DuplicateFinder.Duplicate;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

import static java.util.UUID.randomUUID;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;

@ExtendWith(MockitoExtension.class)
@SuppressWarnings("checkstyle:LineLength")
class DuplicateFinderTest {

    @Mock BlobServiceClient storageClient;
    @Mock EnvelopeService envelopeService;

    @Mock BlobContainerClient blobContainerClient;
    @Mock PagedIterable<BlobItem> listBlobsResult;

    @BeforeEach
    void setUp() {
        given(storageClient.getBlobContainerClient(any())).willReturn(blobContainerClient);
        given(blobContainerClient.listBlobs()).willReturn(listBlobsResult);
    }

    @Test
    void should_return_duplicate_when_envelope_already_exists_and_is_marked_as_deleted() {
        // given
        Envelope deletedEnvelope = new Envelope(randomUUID(), null, null, null, null, null, null, true, false, 0);
        Envelope notYetDeletedEnvelope = new Envelope(randomUUID(), null, null, null, null, null, null, false, false, 0);

        var blobs = Stream.of(
            blob("a.zip"),
            blob("b.zip"),
            blob("c.zip")
        );
        given(listBlobsResult.stream()).willReturn(blobs);

        given(envelopeService.findLastEnvelope("a.zip", "container")).willReturn(Optional.empty());
        given(envelopeService.findLastEnvelope("b.zip", "container")).willReturn(Optional.of(deletedEnvelope));
        given(envelopeService.findLastEnvelope("c.zip", "container")).willReturn(Optional.of(notYetDeletedEnvelope));

        // when
        List<Duplicate> result = new DuplicateFinder(storageClient, envelopeService).findIn("container");

        // then
        assertThat(result)
            .extracting(d -> d.fileName)
            .containsExactly("b.zip");
    }

    private BlobItem blob(String name) {
        var blobItem = mock(BlobItem.class);
        given(blobItem.getName()).willReturn(name);
        var properties = mock(BlobItemProperties.class);

        lenient().when(blobItem.getProperties()).thenReturn(properties);
        lenient().when(properties.getLastModified()).thenReturn(OffsetDateTime.now());

        return blobItem;
    }
}
