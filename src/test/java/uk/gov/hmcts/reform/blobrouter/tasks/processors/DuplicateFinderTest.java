package uk.gov.hmcts.reform.blobrouter.tasks.processors;

import com.azure.core.http.rest.PagedIterable;
import com.azure.storage.blob.BlobContainerClient;
import com.azure.storage.blob.BlobServiceClient;
import com.azure.storage.blob.models.BlobItem;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.reform.blobrouter.data.envelopes.Envelope;
import uk.gov.hmcts.reform.blobrouter.services.EnvelopeService;

import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

import static java.util.UUID.randomUUID;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
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
    void should_return_envelopes_for_which_file_was_found_when_its_already_marked_as_deleted() {
        // given
        Envelope deletedEnvelope = new Envelope(randomUUID(), null, null, null, null, null, null, true);
        Envelope notYetDeletedEnvelope = new Envelope(randomUUID(), null, null, null, null, null, null, false);

        given(listBlobsResult.stream())
            .willReturn(Stream.of(
                blob("a.zip"),
                blob("b.zip"),
                blob("c.zip")
            ));

        given(envelopeService.findEnvelope("a.zip", "container")).willReturn(Optional.empty());
        given(envelopeService.findEnvelope("b.zip", "container")).willReturn(Optional.of(deletedEnvelope));
        given(envelopeService.findEnvelope("c.zip", "container")).willReturn(Optional.of(notYetDeletedEnvelope));

        // when
        List<Envelope> result = new DuplicateFinder(storageClient, envelopeService).findIn("container");

        // then
        assertThat(result)
            .containsExactly(deletedEnvelope);
    }


    private BlobItem blob(String name) {
        var blob = new BlobItem();
        blob.setName(name);
        return blob;
    }

}
