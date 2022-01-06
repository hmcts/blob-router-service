package uk.gov.hmcts.reform.blobrouter.tasks.processors;

import com.azure.storage.blob.BlobClient;
import com.azure.storage.blob.models.BlobProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.reform.blobrouter.config.ServiceConfiguration;
import uk.gov.hmcts.reform.blobrouter.config.StorageConfigItem;
import uk.gov.hmcts.reform.blobrouter.config.TargetStorageAccount;
import uk.gov.hmcts.reform.blobrouter.data.envelopes.Envelope;
import uk.gov.hmcts.reform.blobrouter.data.envelopes.Status;
import uk.gov.hmcts.reform.blobrouter.data.events.ErrorCode;
import uk.gov.hmcts.reform.blobrouter.services.BlobVerifier;
import uk.gov.hmcts.reform.blobrouter.services.EnvelopeService;
import uk.gov.hmcts.reform.blobrouter.services.storage.BlobDispatcher;

import java.io.OutputStream;
import java.time.OffsetDateTime;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.will;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static uk.gov.hmcts.reform.blobrouter.config.TargetStorageAccount.CFT;
import static uk.gov.hmcts.reform.blobrouter.config.TargetStorageAccount.CRIME;
import static uk.gov.hmcts.reform.blobrouter.services.BlobVerifier.INVALID_SIGNATURE_VERIFICATION_RESULT;
import static uk.gov.hmcts.reform.blobrouter.services.BlobVerifier.VerificationResult.OK_VERIFICATION_RESULT;

@ExtendWith(MockitoExtension.class)
@SuppressWarnings("checkstyle:variabledeclarationusagedistance")
class BlobProcessorContinuationTest {

    @Mock BlobDispatcher blobDispatcher;
    @Mock EnvelopeService envelopeService;
    @Mock BlobVerifier verifier;
    @Mock ServiceConfiguration serviceConfiguration;
    @Mock(lenient = true) BlobClient blobClient;

    BlobProcessor blobProcessor;

    @BeforeEach
    void setUp() {
        given(serviceConfiguration.getStorageConfig())
            .willReturn(Map.of(
                "s1", cfg("s1", "t1", CFT),
                "s2", cfg("s2", "t2", CRIME)
            ));
        blobProcessor = new BlobProcessor(
            blobDispatcher,
            envelopeService,
            verifier,
            serviceConfiguration
        );
    }

    @Test
    void should_continue_processing_valid_envelope() {
        // given
        var id = UUID.randomUUID();
        var fileName = "hello.zip";
        var containerName = "s1";

        blobExists(fileName, containerName);
        given(verifier.verifyZip(any(), any())).willReturn(OK_VERIFICATION_RESULT);

        Envelope envelope = envelope(id, Status.CREATED);
        given(envelopeService.findLastEnvelope(fileName, containerName))
            .willReturn(Optional.of(envelope));

        // when
        blobProcessor.process(blobClient);

        // then
        verify(envelopeService, never()).createNewEnvelope(any(), any(), any());
        verify(envelopeService).markAsDispatched(id);
        verify(blobDispatcher).dispatch(blobClient,"t1", CFT);
    }

    @Test
    void should_not_continue_processing_when_envelope_status_is_not_created() {
        // given
        var id = UUID.randomUUID();
        var fileName = "hello.zip";
        var containerName = "s1";

        blobExists(fileName, containerName);

        Envelope envelope = envelope(id, Status.DISPATCHED);
        given(envelopeService.findLastEnvelope(fileName, containerName))
            .willReturn(Optional.of(envelope));

        // when
        blobProcessor.process(blobClient);

        // then
        verify(envelopeService).findLastEnvelope(fileName, containerName);
        verifyNoMoreInteractions(envelopeService);
        verifyNoInteractions(blobDispatcher);
    }

    @Test
    void should_create_envelope_record_when_not_exists() {
        // given
        var id = UUID.randomUUID();
        var fileName = "hello.zip";
        var containerName = "s1";
        OffsetDateTime blobCreationTime = OffsetDateTime.now();

        blobExists(fileName, containerName);
        given(verifier.verifyZip(any(), any())).willReturn(OK_VERIFICATION_RESULT);

        var blobProperties = mock(BlobProperties.class);
        given(blobClient.getProperties()).willReturn(blobProperties);
        given(blobProperties.getCreationTime()).willReturn(blobCreationTime);

        given(envelopeService.findLastEnvelope(fileName, containerName))
            .willReturn(Optional.empty());

        given(envelopeService.createNewEnvelope(containerName, fileName, blobCreationTime.toInstant()))
            .willReturn(id);

        // when
        blobProcessor.process(blobClient);

        // then
        verify(envelopeService).createNewEnvelope(containerName, fileName, blobCreationTime.toInstant());
        verify(envelopeService).markAsDispatched(id);
        verifyNoMoreInteractions(envelopeService);
        verify(blobDispatcher).dispatch(blobClient,"t1", CFT);
    }

    @Test
    void should_reject_invalid_envelope() {
        // given
        var id = UUID.randomUUID();
        var fileName = "hello.zip";
        var containerName = "s1";

        blobExists(fileName, containerName);
        given(verifier.verifyZip(any(), any())).willReturn(INVALID_SIGNATURE_VERIFICATION_RESULT);
        Envelope envelope = envelope(id, Status.CREATED);
        given(envelopeService.findLastEnvelope(fileName, containerName))
            .willReturn(Optional.of(envelope));

        // when
        blobProcessor.process(blobClient);

        // then
        verify(envelopeService).markAsRejected(id, ErrorCode.ERR_SIG_VERIFY_FAILED, "Invalid signature");
        verifyNoMoreInteractions(envelopeService);

        verifyNoInteractions(blobDispatcher);
    }

    private StorageConfigItem cfg(String source, String target, TargetStorageAccount targetAccount) {
        var cfg = new StorageConfigItem();
        cfg.setSourceContainer(source);
        cfg.setTargetContainer(target);
        cfg.setTargetStorageAccount(targetAccount);
        cfg.setEnabled(true);
        return cfg;
    }

    private void blobExists(String blobName, String containerName) {
        given(blobClient.getBlobName()).willReturn(blobName);
        given(blobClient.getContainerName()).willReturn(containerName);

        will(invocation -> {
            var outputStream = (OutputStream) invocation.getArguments()[0];
            outputStream.write("some content".getBytes());
            return null;
        })
            .given(blobClient)
            .download(any());
    }

    private Envelope envelope(UUID id, Status status) {
        return new Envelope(id, "s1", "hello.zip", null, null, null, status, false, false, null);
    }
}
