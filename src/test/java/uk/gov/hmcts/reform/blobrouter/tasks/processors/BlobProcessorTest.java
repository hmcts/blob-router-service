package uk.gov.hmcts.reform.blobrouter.tasks.processors;

import com.azure.core.http.HttpResponse;
import com.azure.storage.blob.BlobClient;
import com.azure.storage.blob.models.BlobProperties;
import com.azure.storage.blob.models.BlobStorageException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.reform.blobrouter.config.ServiceConfiguration;
import uk.gov.hmcts.reform.blobrouter.config.StorageConfigItem;
import uk.gov.hmcts.reform.blobrouter.config.TargetStorageAccount;
import uk.gov.hmcts.reform.blobrouter.data.events.ErrorCode;
import uk.gov.hmcts.reform.blobrouter.data.events.EventType;
import uk.gov.hmcts.reform.blobrouter.services.BlobContentExtractor;
import uk.gov.hmcts.reform.blobrouter.services.BlobVerifier;
import uk.gov.hmcts.reform.blobrouter.services.EnvelopeService;
import uk.gov.hmcts.reform.blobrouter.services.storage.BlobDispatcher;

import java.io.OutputStream;
import java.time.OffsetDateTime;
import java.util.Map;
import java.util.UUID;

import static org.mockito.AdditionalMatchers.aryEq;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.will;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.http.HttpStatus.BAD_GATEWAY;
import static uk.gov.hmcts.reform.blobrouter.config.TargetStorageAccount.BULKSCAN;
import static uk.gov.hmcts.reform.blobrouter.services.BlobVerifier.VerificationResult.error;
import static uk.gov.hmcts.reform.blobrouter.services.BlobVerifier.VerificationResult.ok;

@ExtendWith(MockitoExtension.class)
class BlobProcessorTest {

    private static final String SOURCE_CONTAINER = "sourceContainer1";
    private static final String TARGET_CONTAINER = "targetContainer1";
    private static final TargetStorageAccount TARGET_STORAGE_ACCOUNT = BULKSCAN;

    @Mock(lenient = true) BlobClient blobClient;
    @Mock(lenient = true) BlobProperties blobProperties;
    @Mock BlobDispatcher blobDispatcher;
    @Mock EnvelopeService envelopeService;
    @Mock BlobVerifier verifier;
    @Mock ServiceConfiguration serviceConfiguration;
    @Mock BlobContentExtractor blobContentExtractor;

    @Test
    void should_not_update_envelope_status_when_upload_failed() {
        // given
        var id = UUID.randomUUID();
        given(envelopeService.createNewEnvelope(any(), any(), any())).willReturn(id);
        blobExists("envelope1.zip", SOURCE_CONTAINER);
        setupContainerConfig(SOURCE_CONTAINER, TARGET_CONTAINER, BULKSCAN);
        given(verifier.verifyZip(any(), any())).willReturn(ok());

        willThrow(new RuntimeException("Exception message"))
            .given(blobDispatcher)
            .dispatch(any(), any(), any(), any());

        // when
        newBlobProcessor().process(blobClient);

        // then
        verifyNewEnvelopeHasBeenCreated();

        // dispatcher has been called
        verify(blobDispatcher).dispatch(eq("envelope1.zip"), any(), eq(TARGET_CONTAINER), eq(TARGET_STORAGE_ACCOUNT));

        // but the envelope has not been marked as dispatched
        verify(envelopeService, never()).markAsDispatched(any());

        // and error event has been created
        verify(envelopeService).saveEvent(id, EventType.ERROR, "Exception message");
    }

    @Test
    void should_not_update_envelope_status_when_blob_is_blocked_for_download() {
        // given
        var id = UUID.randomUUID();
        given(envelopeService.createNewEnvelope(any(), any(), any())).willReturn(id);
        blobExists("envelope1.zip", SOURCE_CONTAINER);
        setupContainerConfig(SOURCE_CONTAINER, TARGET_CONTAINER, BULKSCAN);

        HttpResponse errorResponse = mock(HttpResponse.class);
        given(errorResponse.getStatusCode()).willReturn(BAD_GATEWAY.value());

        willThrow(new BlobStorageException("test", errorResponse, null))
            .given(blobClient)
            .download(any());

        // when
        newBlobProcessor().process(blobClient);

        // then
        verifyNewEnvelopeHasBeenCreated();

        verify(blobDispatcher, never()).dispatch(any(), any(), any(), any());

        // but the envelope has not been marked as dispatched
        verify(envelopeService, never()).markAsDispatched(any());

        // and error event has been created
        verify(envelopeService).saveEvent(id, EventType.ERROR, BlobProcessor.ErrorMessages.DOWNLOAD_ERROR_BAD_GATEWAY);
    }

    @Test
    void should_not_update_envelope_status_when_blob_download_fails_for_unknown_reason() {
        // given
        var id = UUID.randomUUID();
        given(envelopeService.createNewEnvelope(any(), any(), any())).willReturn(id);
        blobExists("envelope1.zip", SOURCE_CONTAINER);
        setupContainerConfig(SOURCE_CONTAINER, TARGET_CONTAINER, BULKSCAN);

        willThrow(new RuntimeException("test")).given(blobClient).download(any());

        // when
        newBlobProcessor().process(blobClient);

        // then
        verifyNewEnvelopeHasBeenCreated();

        verify(blobDispatcher, never()).dispatch(any(), any(), any(), any());

        // but the envelope has not been marked as dispatched
        verify(envelopeService, never()).markAsDispatched(any());

        // and error event has been created
        verify(envelopeService).saveEvent(id, EventType.ERROR, BlobProcessor.ErrorMessages.DOWNLOAD_ERROR_GENERIC);
    }

    @Test
    void should_dispatch_valid_file() {
        // given
        var id = UUID.randomUUID();
        given(envelopeService.createNewEnvelope(any(), any(), any())).willReturn(id);
        setupContainerConfig(SOURCE_CONTAINER, TARGET_CONTAINER, BULKSCAN);

        OffsetDateTime blobCreationTime = OffsetDateTime.now();
        String fileName = "envelope1.zip";
        blobExists(fileName, SOURCE_CONTAINER, blobCreationTime);

        given(verifier.verifyZip(any(), any())).willReturn(ok());

        // when
        newBlobProcessor().process(blobClient);

        // then
        verify(blobDispatcher, times(1)).dispatch(any(), any(), any(), any());
        verifyNewEnvelopeHasBeenCreated();
        verify(envelopeService).markAsDispatched(id);
    }

    @Test
    void should_reject_file_if_file_verification_fails() {
        // given
        var id = UUID.randomUUID();
        given(envelopeService.createNewEnvelope(any(), any(), any())).willReturn(id);

        setupContainerConfig(SOURCE_CONTAINER, TARGET_CONTAINER, BULKSCAN);

        OffsetDateTime blobCreationTime = OffsetDateTime.now();
        String fileName = "envelope1.zip";
        blobExists(fileName, SOURCE_CONTAINER, blobCreationTime);

        given(verifier.verifyZip(any(), any())).willReturn(error(ErrorCode.ERR_SIG_VERIFY_FAILED, "some error"));

        // when
        newBlobProcessor().process(blobClient);

        // then
        verifyNoInteractions(blobDispatcher);
        verifyNewEnvelopeHasBeenCreated();
        verify(envelopeService).markAsRejected(id, ErrorCode.ERR_SIG_VERIFY_FAILED, "some error");
    }

    @Test
    void should_upload_the_downloaded_blob_when_target_account_is_bulk_scan() throws Exception {
        // given
        var fileName = "envelope1.zip";
        var sourceContainerName = "sourceContainer1";
        var targetContainerName = "targetContainer1";
        var content = "some zip file content".getBytes();

        setupContainerConfig(sourceContainerName, targetContainerName, BULKSCAN);
        blobExists(fileName, sourceContainerName);
        given(blobContentExtractor.getContentToUpload(any(), any())).willReturn(content);

        var id = UUID.randomUUID();
        given(envelopeService.createNewEnvelope(any(), any(), any())).willReturn(id);

        // valid file
        given(verifier.verifyZip(any(), any())).willReturn(ok());

        // when
        newBlobProcessor().process(blobClient);

        // then
        verifyNewEnvelopeHasBeenCreated();
        verify(blobDispatcher, times(1))
            .dispatch(eq(fileName), aryEq(content), eq(targetContainerName), eq(BULKSCAN));
        verify(envelopeService).markAsDispatched(id);
    }

    private void blobExists(String blobName, String containerName) {
        blobExists(blobName, containerName, OffsetDateTime.now());
    }

    private void blobExists(String blobName, String containerName, OffsetDateTime time) {
        given(blobClient.getBlobName()).willReturn(blobName);
        given(blobClient.getContainerName()).willReturn(containerName);
        given(blobClient.getProperties()).willReturn(blobProperties);

        setupDownloadedBlobContent("some content".getBytes());

        given(blobProperties.getLastModified()).willReturn(time);
    }

    private void setupDownloadedBlobContent(byte[] content) {
        will(invocation -> {
            OutputStream outputStream = (OutputStream) invocation.getArguments()[0];
            outputStream.write(content);
            return null;
        })
            .given(blobClient)
            .download(any());
    }

    private void setupContainerConfig(
        String sourceContainer,
        String targetContainer,
        TargetStorageAccount targetStorageAccount
    ) {
        var containerConfig = new StorageConfigItem();
        containerConfig.setEnabled(true);
        containerConfig.setSourceContainer(sourceContainer);
        containerConfig.setTargetContainer(targetContainer);
        containerConfig.setTargetStorageAccount(targetStorageAccount);

        given(serviceConfiguration.getStorageConfig()).willReturn(Map.of(sourceContainer, containerConfig));
    }

    private void verifyNewEnvelopeHasBeenCreated() {
        verify(envelopeService).createNewEnvelope(
            blobClient.getContainerName(),
            blobClient.getBlobName(),
            blobClient.getProperties().getLastModified().toInstant()
        );
    }

    private BlobProcessor newBlobProcessor() {
        return new BlobProcessor(
            this.blobDispatcher,
            this.envelopeService,
            this.verifier,
            this.blobContentExtractor,
            this.serviceConfiguration
        );
    }
}
