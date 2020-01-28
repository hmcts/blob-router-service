package uk.gov.hmcts.reform.blobrouter.tasks.processors;

import com.azure.storage.blob.BlobClient;
import com.azure.storage.blob.BlobContainerClient;
import com.azure.storage.blob.BlobServiceClient;
import com.azure.storage.blob.models.BlobProperties;
import com.azure.storage.blob.specialized.BlobLeaseClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.reform.blobrouter.data.EnvelopeRepository;
import uk.gov.hmcts.reform.blobrouter.data.model.NewEnvelope;
import uk.gov.hmcts.reform.blobrouter.services.BlobReadinessChecker;
import uk.gov.hmcts.reform.blobrouter.services.BlobSignatureVerifier;
import uk.gov.hmcts.reform.blobrouter.services.storage.BlobDispatcher;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static uk.gov.hmcts.reform.blobrouter.config.TargetStorageAccount.BULKSCAN;
import static uk.gov.hmcts.reform.blobrouter.data.model.Status.DISPATCHED;
import static uk.gov.hmcts.reform.blobrouter.data.model.Status.REJECTED;

@ExtendWith(MockitoExtension.class)
class BlobProcessorTest {

    @Mock BlobReadinessChecker readinessChecker;
    @Mock BlobServiceClient blobServiceClient;
    @Mock BlobContainerClient containerClient;
    @Mock BlobClient blobClient;
    @Mock BlobProperties blobProperties;
    @Mock BlobDispatcher blobDispatcher;
    @Mock BlobLeaseClient blobLeaseClient;
    @Mock EnvelopeRepository envelopeRepo;
    @Mock BlobSignatureVerifier signatureVerifier;

    BlobProcessor blobProcessor;

    @BeforeEach
    void setUp() {
        this.blobProcessor = new BlobProcessor(
            this.blobServiceClient,
            this.blobDispatcher,
            this.readinessChecker,
            this.envelopeRepo,
            blobClient -> blobLeaseClient,
            this.signatureVerifier
        );
    }

    @Test
    void should_not_process_file_if_it_is_not_ready_yet() {
        // given
        OffsetDateTime blobCreationTime = OffsetDateTime.now();

        blobExists(blobCreationTime);

        // file is NOT ready to be processed
        given(readinessChecker.isReady(any())).willReturn(false);

        // when
        blobProcessor.process("hello.zip", "my_container");

        // then
        verify(readinessChecker).isReady(eq(blobCreationTime.toInstant()));
        verify(blobDispatcher, never()).dispatch(any(), any(), any(), any());
    }

    @Test
    void should_not_store_envelope_in_db_when_upload_failed() {
        // given
        blobExists(OffsetDateTime.now());
        given(readinessChecker.isReady(any())).willReturn(true);
        given(signatureVerifier.verifyZipSignature(any(), any(), any())).willReturn(true);

        willThrow(new RuntimeException("Test exception"))
            .given(blobDispatcher)
            .dispatch(any(), any(), any(), any());

        // when
        blobProcessor.process("envelope1.zip", "container1");

        // then
        verify(blobDispatcher).dispatch(eq("envelope1.zip"), any(), eq("container1"), eq(BULKSCAN));
        verify(envelopeRepo, never()).insert(any());
    }

    @Test
    void should_process_file_if_it_is_ready() {
        // given
        OffsetDateTime blobCreationTime = OffsetDateTime.now();
        blobExists(blobCreationTime);

        String fileName = "envelope1.zip";
        String containerName = "container1";

        // file IS ready to be processed
        given(readinessChecker.isReady(any())).willReturn(true);
        given(signatureVerifier.verifyZipSignature(any(), any(), any())).willReturn(true);

        // when
        blobProcessor.process(fileName, containerName);

        // then
        verify(readinessChecker).isReady(eq(blobCreationTime.toInstant()));
        verify(blobDispatcher, times(1)).dispatch(any(), any(), any(), any());

        ArgumentCaptor<NewEnvelope> newEnvelopeArgumentCaptor = ArgumentCaptor.forClass(NewEnvelope.class);
        verify(envelopeRepo).insert(newEnvelopeArgumentCaptor.capture());

        assertThat(newEnvelopeArgumentCaptor.getValue().fileName).isEqualTo(fileName);
        assertThat(newEnvelopeArgumentCaptor.getValue().container).isEqualTo(containerName);
        assertThat(newEnvelopeArgumentCaptor.getValue().dispatchedAt).isBeforeOrEqualTo(Instant.now());
        assertThat(newEnvelopeArgumentCaptor.getValue().fileCreatedAt).isBeforeOrEqualTo(Instant.now());
        assertThat(newEnvelopeArgumentCaptor.getValue().status).isEqualTo(DISPATCHED);
    }

    @Test
    void should_reject_file_if_signature_verification_fails() {
        // given
        OffsetDateTime blobCreationTime = OffsetDateTime.now();
        blobExists(blobCreationTime);

        String fileName = "envelope1.zip";
        String containerName = "container1";

        // file IS ready to be processed
        given(readinessChecker.isReady(any())).willReturn(true);
        given(signatureVerifier.verifyZipSignature(any(), any(), any())).willReturn(false);

        // when
        blobProcessor.process(fileName, containerName);

        // then
        verify(readinessChecker).isReady(eq(blobCreationTime.toInstant()));
        verifyNoInteractions(blobDispatcher);

        ArgumentCaptor<NewEnvelope> newEnvelopeArgumentCaptor = ArgumentCaptor.forClass(NewEnvelope.class);
        verify(envelopeRepo).insert(newEnvelopeArgumentCaptor.capture());

        assertThat(newEnvelopeArgumentCaptor.getValue().fileName).isEqualTo(fileName);
        assertThat(newEnvelopeArgumentCaptor.getValue().container).isEqualTo(containerName);
        assertThat(newEnvelopeArgumentCaptor.getValue().dispatchedAt).isBeforeOrEqualTo(Instant.now());
        assertThat(newEnvelopeArgumentCaptor.getValue().fileCreatedAt).isBeforeOrEqualTo(Instant.now());
        assertThat(newEnvelopeArgumentCaptor.getValue().status).isEqualTo(REJECTED);
    }

    private void blobExists(OffsetDateTime time) {
        given(blobServiceClient.getBlobContainerClient(any())).willReturn(containerClient);
        given(containerClient.getBlobClient(any())).willReturn(blobClient);
        given(blobClient.getProperties()).willReturn(blobProperties);
        given(blobProperties.getLastModified()).willReturn(time);
        given(envelopeRepo.find(any(), any())).willReturn(Optional.empty());
    }
}
