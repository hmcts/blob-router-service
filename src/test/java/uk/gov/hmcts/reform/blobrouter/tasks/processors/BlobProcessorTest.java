package uk.gov.hmcts.reform.blobrouter.tasks.processors;

import com.azure.storage.blob.BlobClient;
import com.azure.storage.blob.BlobContainerClient;
import com.azure.storage.blob.BlobServiceClient;
import com.azure.storage.blob.models.BlobProperties;
import com.azure.storage.blob.specialized.BlobLeaseClient;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.reform.blobrouter.config.ServiceConfiguration;
import uk.gov.hmcts.reform.blobrouter.config.TargetStorageAccount;
import uk.gov.hmcts.reform.blobrouter.data.EnvelopeRepository;
import uk.gov.hmcts.reform.blobrouter.data.model.NewEnvelope;
import uk.gov.hmcts.reform.blobrouter.services.BlobReadinessChecker;
import uk.gov.hmcts.reform.blobrouter.services.BlobSignatureVerifier;
import uk.gov.hmcts.reform.blobrouter.services.storage.BlobDispatcher;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.Map;
import java.util.Optional;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.AdditionalMatchers.aryEq;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.will;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static uk.gov.hmcts.reform.blobrouter.config.TargetStorageAccount.BULKSCAN;
import static uk.gov.hmcts.reform.blobrouter.config.TargetStorageAccount.CRIME;
import static uk.gov.hmcts.reform.blobrouter.data.model.Status.DISPATCHED;
import static uk.gov.hmcts.reform.blobrouter.data.model.Status.REJECTED;

@ExtendWith(MockitoExtension.class)
class BlobProcessorTest {

    private static final String SOURCE_CONTAINER = "sourceContainer1";
    private static final String TARGET_CONTAINER = "targetContainer1";
    private static final TargetStorageAccount TARGET_STORAGE_ACCOUNT = BULKSCAN;

    private static final byte[] INTERNAL_ENVELOPE_CONTENT = "envelope content is irrelevant".getBytes();
    private static final byte[] BLOB_CONTENT = getBlobContent(INTERNAL_ENVELOPE_CONTENT);

    @Mock BlobReadinessChecker readinessChecker;
    @Mock BlobServiceClient blobServiceClient;
    @Mock BlobContainerClient containerClient;
    @Mock BlobClient blobClient;
    @Mock BlobProperties blobProperties;
    @Mock BlobDispatcher blobDispatcher;
    @Mock BlobLeaseClient blobLeaseClient;
    @Mock EnvelopeRepository envelopeRepo;
    @Mock BlobSignatureVerifier signatureVerifier;
    @Mock ServiceConfiguration serviceConfiguration;

    @Test
    void should_not_process_file_if_it_is_not_ready_yet() {
        // given
        OffsetDateTime blobCreationTime = OffsetDateTime.now();
        blobExists(blobCreationTime, false);

        // file is NOT ready to be processed
        given(readinessChecker.isReady(any())).willReturn(false);

        // when
        newBlobProcessor().process("hello.zip", SOURCE_CONTAINER);

        // then
        verify(readinessChecker).isReady(eq(blobCreationTime.toInstant()));
        verify(blobDispatcher, never()).dispatch(any(), any(), any(), any());
    }

    @Test
    void should_not_store_envelope_in_db_when_upload_failed() {
        // given
        blobExists();
        setupContainerConfig(SOURCE_CONTAINER, TARGET_CONTAINER, BULKSCAN);
        given(readinessChecker.isReady(any())).willReturn(true);
        given(signatureVerifier.verifyZipSignature(any(), any())).willReturn(true);

        willThrow(new RuntimeException("Test exception"))
            .given(blobDispatcher)
            .dispatch(any(), any(), any(), any());

        // when
        newBlobProcessor().process("envelope1.zip", SOURCE_CONTAINER);

        // then
        verify(blobDispatcher).dispatch(eq("envelope1.zip"), any(), eq(TARGET_CONTAINER), eq(TARGET_STORAGE_ACCOUNT));
        verify(envelopeRepo, never()).insert(any());
    }

    @Test
    void should_process_file_if_it_is_ready() {
        // given
        setupContainerConfig(SOURCE_CONTAINER, TARGET_CONTAINER, BULKSCAN);

        OffsetDateTime blobCreationTime = OffsetDateTime.now();
        blobExists(blobCreationTime, true);

        String fileName = "envelope1.zip";
        String containerName = SOURCE_CONTAINER;

        // file IS ready to be processed
        given(readinessChecker.isReady(any())).willReturn(true);
        given(signatureVerifier.verifyZipSignature(any(), any())).willReturn(true);

        // when
        newBlobProcessor().process(fileName, containerName);

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
        setupContainerConfig(SOURCE_CONTAINER, TARGET_CONTAINER, BULKSCAN);

        OffsetDateTime blobCreationTime = OffsetDateTime.now();
        blobExists(blobCreationTime, true);

        String fileName = "envelope1.zip";
        String containerName = "container1";

        // file IS ready to be processed
        given(readinessChecker.isReady(any())).willReturn(true);
        given(signatureVerifier.verifyZipSignature(any(), any())).willReturn(false);

        // when
        newBlobProcessor().process(fileName, containerName);

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

    @Test
    void should_upload_the_downloaded_blob_when_target_account_is_bulk_scan() {
        // given
        blobExists();
        var sourceContainerName = "sourceContainer1";
        var targetContainerName = "targetContainer1";

        setupContainerConfig(sourceContainerName, targetContainerName, BULKSCAN);

        // valid file, ready to be processed
        given(readinessChecker.isReady(any())).willReturn(true);
        given(signatureVerifier.verifyZipSignature(any(), any())).willReturn(true);

        var fileName = "envelope1.zip";

        // when
        newBlobProcessor().process(fileName, sourceContainerName);

        // then
        verify(blobDispatcher, times(1))
            .dispatch(eq(fileName), aryEq(BLOB_CONTENT), eq(targetContainerName), eq(BULKSCAN));
    }

    @Test
    void should_upload_extracted_envelope_when_target_account_is_crime() {
        // given
        blobExists();
        var sourceContainerName = "sourceContainer1";
        var targetContainerName = "targetContainer1";

        setupContainerConfig(sourceContainerName, targetContainerName, CRIME);

        // valid file, ready to be processed
        given(readinessChecker.isReady(any())).willReturn(true);
        given(signatureVerifier.verifyZipSignature(any(), any())).willReturn(true);
        var fileName = "envelope1.zip";

        // when
        newBlobProcessor().process(fileName, sourceContainerName);

        // then
        verify(blobDispatcher, times(1))
            .dispatch(eq(fileName), aryEq(INTERNAL_ENVELOPE_CONTENT), eq(targetContainerName), eq(CRIME));
    }

    private static byte[] getBlobContent(byte[] internalEnvelopeContent) {
        try (
            var outputStream = new ByteArrayOutputStream();
            var zipOutputStream = new ZipOutputStream(outputStream)
        ) {
            zipOutputStream.putNextEntry(new ZipEntry("envelope.zip"));
            zipOutputStream.write(internalEnvelopeContent);
            zipOutputStream.closeEntry();

            zipOutputStream.putNextEntry(new ZipEntry("signature"));
            zipOutputStream.write("signature content is irrelevant".getBytes());
            zipOutputStream.closeEntry();

            return outputStream.toByteArray();
        } catch (IOException e) {
            throw new RuntimeException("Failed to create test blob content", e);
        }
    }

    private void blobExists() {
        blobExists(OffsetDateTime.now(), true);
    }

    private void blobExists(OffsetDateTime time, boolean setupContent) {
        given(blobServiceClient.getBlobContainerClient(any())).willReturn(containerClient);
        given(containerClient.getBlobClient(any())).willReturn(blobClient);
        given(blobClient.getProperties()).willReturn(blobProperties);

        if (setupContent) {
            will(invocation -> {
                OutputStream outputStream = (OutputStream) invocation.getArguments()[0];
                outputStream.write(BLOB_CONTENT);
                return null;
            }).given(blobClient).download(any());
        }

        given(blobProperties.getLastModified()).willReturn(time);
        given(envelopeRepo.find(any(), any())).willReturn(Optional.empty());
    }

    private void setupContainerConfig(
        String sourceContainer,
        String targetContainer,
        TargetStorageAccount targetStorageAccount
    ) {
        ServiceConfiguration.StorageConfig containerConfig = new ServiceConfiguration.StorageConfig();
        containerConfig.setEnabled(true);
        containerConfig.setName(sourceContainer);
        containerConfig.setTargetContainer(targetContainer);
        containerConfig.setTargetStorageAccount(targetStorageAccount);

        given(serviceConfiguration.getStorageConfig()).willReturn(Map.of(sourceContainer, containerConfig));
    }

    private BlobProcessor newBlobProcessor() {
        return new BlobProcessor(
            this.blobServiceClient,
            this.blobDispatcher,
            this.readinessChecker,
            this.envelopeRepo,
            blobClient -> blobLeaseClient,
            this.signatureVerifier,
            this.serviceConfiguration
        );
    }
}
