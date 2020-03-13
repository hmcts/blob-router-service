package uk.gov.hmcts.reform.blobrouter.tasks.processors;

import com.azure.core.http.rest.PagedIterable;
import com.azure.storage.blob.BlobClient;
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
import uk.gov.hmcts.reform.blobrouter.data.envelopes.Status;
import uk.gov.hmcts.reform.blobrouter.services.BlobReadinessChecker;
import uk.gov.hmcts.reform.blobrouter.services.EnvelopeService;

import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Stream;

import static java.time.Instant.now;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;

@ExtendWith(MockitoExtension.class)
class ContainerProcessorTest {

    @Mock BlobServiceClient storageClient;
    @Mock BlobProcessor blobProcessor;
    @Mock BlobReadinessChecker blobReadinessChecker;
    @Mock EnvelopeService envelopeService;

    @Mock BlobContainerClient containerClient;
    @Mock BlobClient blobClient;
    @Mock PagedIterable<BlobItem> listBlobsResult;

    ContainerProcessor containerProcessor;

    @BeforeEach
    void setUp() {
        containerProcessor = new ContainerProcessor(
            storageClient,
            blobProcessor,
            blobReadinessChecker,
            envelopeService
        );
    }

    @Test
    void should_continue_processing_blob_for_which_envelope_already_exists() {
        // given
        var envelope = new Envelope(
            UUID.randomUUID(),
            "some_container",
            "hello.zip",
            now(),
            null,
            null,
            Status.CREATED,
            false
        );

        storageHasBlob(envelope.fileName, envelope.container);
        dbHas(envelope);

        // when
        containerProcessor.process(envelope.container);

        // then
        verify(blobProcessor).continueProcessing(envelope.id, blobClient);
        verifyNoMoreInteractions(blobProcessor);
    }

    @Test
    void should_process_blob_if_envelope_does_not_exist_yet() {
        // given
        storageHasBlob("x.zip", "container");
        given(envelopeService.findEnvelope(any(), any())).willReturn(Optional.empty());

        // when
        containerProcessor.process("container");

        // then
        verify(blobProcessor).process(blobClient);
        verifyNoMoreInteractions(blobProcessor);
    }

    private void storageHasBlob(String fileName, String containerName) {
        given(storageClient.getBlobContainerClient(containerName)).willReturn(containerClient);
        given(containerClient.listBlobs()).willReturn(listBlobsResult);

        var blob = blob(fileName);
        given(listBlobsResult.stream()).willReturn(Stream.of(blob));
        given(containerClient.getBlobClient(blob.getName())).willReturn(blobClient);
        given(blobClient.getBlobName()).willReturn(fileName);
        given(blobClient.getContainerName()).willReturn(containerName);
        given(blobReadinessChecker.isReady(any())).willReturn(true);
    }

    private void dbHas(Envelope envelope) {
        given(envelopeService.findEnvelope(envelope.fileName, envelope.container))
            .willReturn(Optional.of(envelope));
    }

    private BlobItem blob(String name) {
        var blobItem = mock(BlobItem.class);
        var properties = mock(BlobItemProperties.class);

        given(blobItem.getProperties()).willReturn(properties);
        given(properties.getLastModified()).willReturn(OffsetDateTime.now());
        given(blobItem.getName()).willReturn(name);

        return blobItem;
    }
}
