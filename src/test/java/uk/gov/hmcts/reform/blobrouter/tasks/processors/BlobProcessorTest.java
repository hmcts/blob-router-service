package uk.gov.hmcts.reform.blobrouter.tasks.processors;

import com.azure.storage.blob.BlobClient;
import com.azure.storage.blob.models.BlobProperties;
import com.azure.storage.blob.specialized.BlobLeaseClient;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.reform.blobrouter.config.ServiceConfiguration;
import uk.gov.hmcts.reform.blobrouter.config.StorageConfigItem;
import uk.gov.hmcts.reform.blobrouter.config.TargetStorageAccount;
import uk.gov.hmcts.reform.blobrouter.data.model.Event;
import uk.gov.hmcts.reform.blobrouter.services.BlobVerifier;
import uk.gov.hmcts.reform.blobrouter.services.EnvelopeService;
import uk.gov.hmcts.reform.blobrouter.services.storage.BlobDispatcher;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.time.OffsetDateTime;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import static org.mockito.AdditionalMatchers.aryEq;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.will;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static uk.gov.hmcts.reform.blobrouter.config.TargetStorageAccount.BULKSCAN;
import static uk.gov.hmcts.reform.blobrouter.config.TargetStorageAccount.CRIME;
import static uk.gov.hmcts.reform.blobrouter.services.BlobVerifier.VerificationResult.error;
import static uk.gov.hmcts.reform.blobrouter.services.BlobVerifier.VerificationResult.ok;

@ExtendWith(MockitoExtension.class)
class BlobProcessorTest {

    private static final String SOURCE_CONTAINER = "sourceContainer1";
    private static final String TARGET_CONTAINER = "targetContainer1";
    private static final TargetStorageAccount TARGET_STORAGE_ACCOUNT = BULKSCAN;

    private static final byte[] INTERNAL_ENVELOPE_CONTENT = "envelope content is irrelevant".getBytes();
    private static final byte[] BLOB_CONTENT =
        getBlobContent(
            Map.of(
                "envelope.zip", INTERNAL_ENVELOPE_CONTENT,
                "signature", "content irrelevant".getBytes()
            )
        );

    @Mock(lenient = true) BlobClient blobClient;
    @Mock(lenient = true) BlobProperties blobProperties;
    @Mock BlobDispatcher blobDispatcher;
    @Mock BlobLeaseClient blobLeaseClient;
    @Mock EnvelopeService envelopeService;
    @Mock BlobVerifier verifier;
    @Mock ServiceConfiguration serviceConfiguration;

    @Test
    void should_not_store_envelope_in_db_when_upload_failed() {
        // given
        blobExists("envelope1.zip", SOURCE_CONTAINER);
        setupContainerConfig(SOURCE_CONTAINER, TARGET_CONTAINER, BULKSCAN);
        given(verifier.verifyZip(any(), any())).willReturn(ok());

        willThrow(new RuntimeException("Test exception"))
            .given(blobDispatcher)
            .dispatch(any(), any(), any(), any());

        // when
        newBlobProcessor().process(blobClient);

        // then
        verify(blobDispatcher).dispatch(eq("envelope1.zip"), any(), eq(TARGET_CONTAINER), eq(TARGET_STORAGE_ACCOUNT));
        verify(envelopeService).saveEvent(SOURCE_CONTAINER, "envelope1.zip", Event.FILE_PROCESSING_STARTED);
        verify(envelopeService).saveEvent(SOURCE_CONTAINER, "envelope1.zip", Event.ERROR);
        verify(envelopeService, never()).createDispatchedEnvelope(any(), any(), any());
    }

    @Test
    void should_dispatch_valid_file() {
        // given
        setupContainerConfig(SOURCE_CONTAINER, TARGET_CONTAINER, BULKSCAN);

        OffsetDateTime blobCreationTime = OffsetDateTime.now();
        String fileName = "envelope1.zip";
        blobExists(fileName, SOURCE_CONTAINER, blobCreationTime, BLOB_CONTENT);

        given(verifier.verifyZip(any(), any())).willReturn(ok());

        // when
        newBlobProcessor().process(blobClient);

        // then
        verify(blobDispatcher, times(1)).dispatch(any(), any(), any(), any());
        verify(envelopeService).saveEvent(SOURCE_CONTAINER, "envelope1.zip", Event.FILE_PROCESSING_STARTED);
        verify(envelopeService).createDispatchedEnvelope(eq(SOURCE_CONTAINER), eq(fileName), any());
    }

    @Test
    void should_reject_file_if_file_verification_fails() {
        // given
        setupContainerConfig(SOURCE_CONTAINER, TARGET_CONTAINER, BULKSCAN);

        OffsetDateTime blobCreationTime = OffsetDateTime.now();
        String fileName = "envelope1.zip";
        blobExists(fileName, SOURCE_CONTAINER, blobCreationTime, BLOB_CONTENT);

        given(verifier.verifyZip(any(), any())).willReturn(error("error"));

        // when
        newBlobProcessor().process(blobClient);

        // then
        verifyNoInteractions(blobDispatcher);
        verify(envelopeService).createRejectedEnvelope(
            eq(SOURCE_CONTAINER),
            eq(fileName),
            any(),
            eq("error")
        );
    }

    @Test
    void should_upload_the_downloaded_blob_when_target_account_is_bulk_scan() {
        // given
        var fileName = "envelope1.zip";
        var sourceContainerName = "sourceContainer1";
        var targetContainerName = "targetContainer1";

        setupContainerConfig(sourceContainerName, targetContainerName, BULKSCAN);
        blobExists(fileName, sourceContainerName);

        // valid file
        given(verifier.verifyZip(any(), any())).willReturn(ok());

        // when
        newBlobProcessor().process(blobClient);

        // then
        verify(blobDispatcher, times(1))
            .dispatch(eq(fileName), aryEq(BLOB_CONTENT), eq(targetContainerName), eq(BULKSCAN));
        verify(envelopeService).saveEvent(SOURCE_CONTAINER, "envelope1.zip", Event.FILE_PROCESSING_STARTED);
    }

    @Test
    void should_upload_extracted_envelope_when_target_account_is_crime() {
        // given
        var fileName = "envelope1.zip";
        var sourceContainerName = "sourceContainer1";
        var targetContainerName = "targetContainer1";

        blobExists(fileName, sourceContainerName);

        setupContainerConfig(sourceContainerName, targetContainerName, CRIME);

        // valid file
        given(verifier.verifyZip(any(), any())).willReturn(ok());

        // when
        newBlobProcessor().process(blobClient);

        // then
        verify(blobDispatcher, times(1))
            .dispatch(eq(fileName), aryEq(INTERNAL_ENVELOPE_CONTENT), eq(targetContainerName), eq(CRIME));
        verify(envelopeService).saveEvent(SOURCE_CONTAINER, "envelope1.zip", Event.FILE_PROCESSING_STARTED);
    }

    @Test
    void should_not_upload_to_crime_when_blob_is_not_zip() {
        // given
        var sourceContainerName = "sourceContainer1";
        var targetContainerName = "targetContainer1";

        blobExists("envelope1.zip", sourceContainerName, OffsetDateTime.now(), "not zip file's content".getBytes());

        setupContainerConfig(sourceContainerName, targetContainerName, CRIME);

        // valid file
        given(verifier.verifyZip(any(), any())).willReturn(ok());

        // when
        newBlobProcessor().process(blobClient);

        // then
        verify(blobDispatcher, never()).dispatch(any(), any(), any(), any());
        verify(envelopeService).saveEvent(SOURCE_CONTAINER, "envelope1.zip", Event.FILE_PROCESSING_STARTED);
    }

    @Test
    void should_not_upload_to_crime_when_blob_does_not_contain_envelope_entry() {
        // given
        var sourceContainerName = "sourceContainer1";
        var targetContainerName = "targetContainer1";

        // blob contains signature file only
        blobExists(
            "envelope1.zip",
            sourceContainerName,
            OffsetDateTime.now(),
            getBlobContent(Map.of("signature", "test".getBytes()))
        );

        setupContainerConfig(sourceContainerName, targetContainerName, CRIME);

        // valid file
        given(verifier.verifyZip(any(), any())).willReturn(ok());

        // when
        newBlobProcessor().process(blobClient);

        // then
        verify(blobDispatcher, never()).dispatch(any(), any(), any(), any());
        verify(envelopeService).saveEvent(SOURCE_CONTAINER, "envelope1.zip", Event.FILE_PROCESSING_STARTED);
    }

    @Test
    void should_not_create_events_when_lease_cant_be_acquired() {
        // given
        setupContainerConfig(SOURCE_CONTAINER, TARGET_CONTAINER, BULKSCAN);
        String fileName = "envelope1.zip";
        blobExists(fileName, SOURCE_CONTAINER, OffsetDateTime.now(), BLOB_CONTENT);

        doThrow(RuntimeException.class)
            .when(blobLeaseClient).acquireLease(anyInt());

        // when
        newBlobProcessor().process(blobClient);

        // then
        verify(envelopeService, times(0)).saveEvent(any(), any(), any());
        verify(verifier, times(0)).verifyZip(any(), any());
    }

    private static byte[] getBlobContent(Map<String, byte[]> zipEntries) {
        try (
            var outputStream = new ByteArrayOutputStream();
            var zipOutputStream = new ZipOutputStream(outputStream)
        ) {
            for (var entry : zipEntries.entrySet()) {
                var fileName = entry.getKey();
                var fileBytes = entry.getValue();

                zipOutputStream.putNextEntry(new ZipEntry(fileName));
                zipOutputStream.write(fileBytes);
                zipOutputStream.closeEntry();
            }

            return outputStream.toByteArray();
        } catch (IOException e) {
            throw new RuntimeException("Failed to create test blob content", e);
        }
    }

    private void blobExists(String blobName, String containerName) {
        blobExists(blobName, containerName, OffsetDateTime.now(), BLOB_CONTENT);
    }

    private void blobExists(String blobName, String containerName, OffsetDateTime time, byte[] contentToDownload) {
        given(blobClient.getBlobName()).willReturn(blobName);
        given(blobClient.getContainerName()).willReturn(containerName);
        given(blobClient.getProperties()).willReturn(blobProperties);

        if (contentToDownload != null) {
            setupDownloadedBlobContent(contentToDownload);
        }

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

    private BlobProcessor newBlobProcessor() {
        return new BlobProcessor(
            this.blobDispatcher,
            this.envelopeService,
            blobClient -> blobLeaseClient,
            this.verifier,
            this.serviceConfiguration
        );
    }
}
