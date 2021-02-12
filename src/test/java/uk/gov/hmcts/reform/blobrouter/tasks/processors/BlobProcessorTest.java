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
import uk.gov.hmcts.reform.blobrouter.services.BlobVerifier;
import uk.gov.hmcts.reform.blobrouter.services.EnvelopeService;
import uk.gov.hmcts.reform.blobrouter.services.storage.BlobDispatcher;

import java.io.OutputStream;
import java.time.OffsetDateTime;
import java.util.Map;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
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
import static uk.gov.hmcts.reform.blobrouter.config.TargetStorageAccount.CFT;
import static uk.gov.hmcts.reform.blobrouter.config.TargetStorageAccount.CRIME;
import static uk.gov.hmcts.reform.blobrouter.config.TargetStorageAccount.PCQ;
import static uk.gov.hmcts.reform.blobrouter.services.BlobVerifier.VerificationResult.error;
import static uk.gov.hmcts.reform.blobrouter.services.BlobVerifier.VerificationResult.ok;

@ExtendWith(MockitoExtension.class)
class BlobProcessorTest {

    private static final String SOURCE_CONTAINER = "sourceContainer1";
    private static final String TARGET_CONTAINER = "targetContainer1";
    private static final TargetStorageAccount TARGET_STORAGE_ACCOUNT = CFT;

    @Mock(lenient = true) BlobClient blobClient;
    @Mock(lenient = true) BlobProperties blobProperties;
    @Mock BlobDispatcher blobDispatcher;
    @Mock EnvelopeService envelopeService;
    @Mock BlobVerifier verifier;
    @Mock ServiceConfiguration serviceConfiguration;

    @Test
    void should_not_update_envelope_status_when_move_failed() {
        // given
        var id = UUID.randomUUID();
        given(envelopeService.createNewEnvelope(any(), any(), any())).willReturn(id);
        blobExists("envelope1.zip", SOURCE_CONTAINER);
        setupContainerConfig(SOURCE_CONTAINER, TARGET_CONTAINER, CFT);
        given(verifier.verifyZip(any(), any())).willReturn(ok());

        willThrow(new RuntimeException("Exception message"))
            .given(blobDispatcher)
            .moveBlob(any(), any(), any());

        // when
        newBlobProcessor().process(blobClient);

        // then
        verifyNewEnvelopeHasBeenCreated();

        // dispatcher has been called
        verify(blobDispatcher).moveBlob(eq(blobClient), eq(TARGET_CONTAINER), eq(TARGET_STORAGE_ACCOUNT));

        // but the envelope has not been marked as dispatched
        verify(envelopeService, never()).markAsDispatched(any());

        // and error event has been created
        verify(envelopeService).saveEvent(id, EventType.ERROR, "Exception message");
    }

    @Test
    void should_not_update_envelope_status_when_upload_failed_and_message_with_html_should_be_escaped() {
        // given
        var id = UUID.randomUUID();
        given(envelopeService.createNewEnvelope(any(), any(), any())).willReturn(id);
        blobExists("envelope1.zip", SOURCE_CONTAINER);
        setupContainerConfig(SOURCE_CONTAINER, TARGET_CONTAINER, CRIME);
        given(verifier.verifyZip(any(), any())).willReturn(ok());

        willThrow(new RuntimeException(
            "<html><head><title>Oh no!</title></head><body><h2>You failed</h2></body</html>"
        ))
            .given(blobDispatcher)
            .dispatch(any(), anyString(), any());

        // when
        newBlobProcessor().process(blobClient);

        // then
        verifyNewEnvelopeHasBeenCreated();

        // dispatcher has been called
        verify(blobDispatcher).dispatch(blobClient, TARGET_CONTAINER, CRIME);

        // but the envelope has not been marked as dispatched
        verify(envelopeService, never()).markAsDispatched(any());

        // and error event has been created
        verify(envelopeService).saveEvent(
            id,
            EventType.ERROR,
            "&lt;html&gt;&lt;head&gt;&lt;title&gt;Oh no!&lt;/title&gt;&lt;/head&gt;"
                + "&lt;body&gt;&lt;h2&gt;You failed&lt;/h2&gt;&lt;/body&lt;/html&gt;"
        );
    }

    @Test
    void should_not_update_envelope_status_when_blob_is_blocked_for_download() {
        // given
        var id = UUID.randomUUID();
        given(envelopeService.createNewEnvelope(any(), any(), any())).willReturn(id);
        blobExists("envelope1.zip", SOURCE_CONTAINER);
        setupContainerConfig(SOURCE_CONTAINER, TARGET_CONTAINER, PCQ);

        HttpResponse errorResponse = mock(HttpResponse.class);
        given(errorResponse.getStatusCode()).willReturn(BAD_GATEWAY.value());
        given(verifier.verifyZip(any(), any())).willReturn(ok());

        willThrow(new BlobStorageException("test", errorResponse, null))
            .given(blobDispatcher)
            .dispatch(any(), anyString(), any());

        // when
        newBlobProcessor().process(blobClient);

        // then
        verifyNewEnvelopeHasBeenCreated();

        // but the envelope has not been marked as dispatched
        verify(envelopeService, never()).markAsDispatched(any());

        // and error event has been created
        verify(envelopeService).saveEvent(id, EventType.ERROR,  "test");
    }

    @Test
    void should_not_update_envelope_status_when_blob_move_fails_for_unknown_reason() {
        // given
        var id = UUID.randomUUID();
        given(envelopeService.createNewEnvelope(any(), any(), any())).willReturn(id);
        blobExists("envelope1.zip", SOURCE_CONTAINER);
        setupContainerConfig(SOURCE_CONTAINER, TARGET_CONTAINER, CFT);
        given(verifier.verifyZip(any(), any())).willReturn(ok());

        willThrow(new RuntimeException("test")).given(blobDispatcher).moveBlob(any(), any(), any());

        // when
        newBlobProcessor().process(blobClient);

        // then
        verifyNewEnvelopeHasBeenCreated();


        // but the envelope has not been marked as dispatched
        verify(envelopeService, never()).markAsDispatched(any());

        // and error event has been created
        verify(envelopeService).saveEvent(id, EventType.ERROR, "test");
    }

    @Test
    void should_dispatch_valid_file() {
        // given
        var id = UUID.randomUUID();
        given(envelopeService.createNewEnvelope(any(), any(), any())).willReturn(id);
        setupContainerConfig(SOURCE_CONTAINER, TARGET_CONTAINER, CFT);

        OffsetDateTime blobCreationTime = OffsetDateTime.now();
        String fileName = "envelope1.zip";
        blobExists(fileName, SOURCE_CONTAINER, blobCreationTime);

        given(verifier.verifyZip(any(), any())).willReturn(ok());

        // when
        newBlobProcessor().process(blobClient);

        // then
        verify(blobDispatcher, times(1)).moveBlob(any(), any(), any());
        verifyNewEnvelopeHasBeenCreated();
        verify(envelopeService).markAsDispatched(id);
    }

    @Test
    void should_reject_file_if_file_verification_fails() {
        // given
        var id = UUID.randomUUID();
        given(envelopeService.createNewEnvelope(any(), any(), any())).willReturn(id);

        setupContainerConfig(SOURCE_CONTAINER, TARGET_CONTAINER, CFT);

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
    void should_upload_the_downloaded_blob_when_target_account_is_bulk_scan() {
        // given
        var fileName = "envelope1.zip";
        var sourceContainerName = "sourceContainer1";
        var targetContainerName = "targetContainer1";

        setupContainerConfig(sourceContainerName, targetContainerName, CFT);
        blobExists(fileName, sourceContainerName);

        var id = UUID.randomUUID();
        given(envelopeService.createNewEnvelope(any(), any(), any())).willReturn(id);

        // valid file
        given(verifier.verifyZip(any(), any())).willReturn(ok());

        // when
        newBlobProcessor().process(blobClient);

        // then
        verifyNewEnvelopeHasBeenCreated();
        verify(blobDispatcher, times(1))
            .moveBlob(eq(blobClient), eq(targetContainerName), eq(CFT));
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

    @Test
    void should_stream_content_when_extract_is_enabled_and_target_account_is_cft() {
        // given
        var fileName = "envelope1.zip";
        var sourceContainerName = "sourceContainer1";
        var targetContainerName = "targetContainer1";

        setupContainerConfig(sourceContainerName, targetContainerName, CFT);
        blobExists(fileName, sourceContainerName);

        var id = UUID.randomUUID();
        given(envelopeService.createNewEnvelope(any(), any(), any())).willReturn(id);

        // valid file
        given(verifier.verifyZip(any(), any())).willReturn(ok());

        // when
        newBlobProcessor(true).process(blobClient);

        // then
        verifyNewEnvelopeHasBeenCreated();
        verify(blobDispatcher, times(1))
            .dispatch(eq(blobClient), eq(targetContainerName), eq(CFT));
        verify(envelopeService).markAsDispatched(id);
    }

    private BlobProcessor newBlobProcessor() {
        return newBlobProcessor(false);
    }

    private BlobProcessor newBlobProcessor(boolean extractEnvelopeForCft) {
        return new BlobProcessor(
            this.blobDispatcher,
            this.envelopeService,
            this.verifier,
            this.serviceConfiguration,
            extractEnvelopeForCft
        );
    }
}
