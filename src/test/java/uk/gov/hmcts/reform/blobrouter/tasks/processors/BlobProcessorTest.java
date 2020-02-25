package uk.gov.hmcts.reform.blobrouter.tasks.processors;

import com.azure.storage.blob.BlobClient;
import com.azure.storage.blob.BlobContainerClient;
import com.azure.storage.blob.BlobServiceClient;
import com.azure.storage.blob.models.BlobProperties;
import com.azure.storage.blob.specialized.BlobLeaseClient;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.reform.blobrouter.config.ServiceConfiguration;
import uk.gov.hmcts.reform.blobrouter.config.StorageConfigItem;
import uk.gov.hmcts.reform.blobrouter.config.TargetStorageAccount;
import uk.gov.hmcts.reform.blobrouter.services.BlobVerifier;
import uk.gov.hmcts.reform.blobrouter.services.EnvelopeService;
import uk.gov.hmcts.reform.blobrouter.services.storage.BlobDispatcher;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.time.OffsetDateTime;
import java.util.Map;
import java.util.Optional;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

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

    @Mock BlobServiceClient blobServiceClient;
    @Mock BlobContainerClient containerClient;
    @Mock BlobClient blobClient;
    @Mock BlobProperties blobProperties;
    @Mock BlobDispatcher blobDispatcher;
    @Mock BlobLeaseClient blobLeaseClient;
    @Mock EnvelopeService envelopeService;
    @Mock BlobVerifier verifier;
    @Mock ServiceConfiguration serviceConfiguration;

    @Test
    void should_not_store_envelope_in_db_when_upload_failed() {
        // given
        blobExists();
        setupContainerConfig(SOURCE_CONTAINER, TARGET_CONTAINER, BULKSCAN);
        given(verifier.verifyZip(any(), any())).willReturn(ok());

        willThrow(new RuntimeException("Test exception"))
            .given(blobDispatcher)
            .dispatch(any(), any(), any(), any());

        // when
        newBlobProcessor().process("envelope1.zip", SOURCE_CONTAINER);

        // then
        verify(blobDispatcher).dispatch(eq("envelope1.zip"), any(), eq(TARGET_CONTAINER), eq(TARGET_STORAGE_ACCOUNT));
        verify(envelopeService, never()).createDispatchedEnvelope(any(), any(), any());
    }

    @Test
    void should_dispatch_valid_file() {
        // given
        setupContainerConfig(SOURCE_CONTAINER, TARGET_CONTAINER, BULKSCAN);

        OffsetDateTime blobCreationTime = OffsetDateTime.now();
        blobExists(blobCreationTime, BLOB_CONTENT);

        String fileName = "envelope1.zip";

        given(verifier.verifyZip(any(), any())).willReturn(ok());

        // when
        newBlobProcessor().process(fileName, SOURCE_CONTAINER);

        // then
        verify(blobDispatcher, times(1)).dispatch(any(), any(), any(), any());
        verify(envelopeService).createDispatchedEnvelope(eq(SOURCE_CONTAINER), eq(fileName), any());
    }

    @Test
    void should_reject_file_if_file_verification_fails() {
        // given
        setupContainerConfig(SOURCE_CONTAINER, TARGET_CONTAINER, BULKSCAN);

        OffsetDateTime blobCreationTime = OffsetDateTime.now();
        blobExists(blobCreationTime, BLOB_CONTENT);

        String fileName = "envelope1.zip";

        given(verifier.verifyZip(any(), any())).willReturn(error("error"));

        // when
        newBlobProcessor().process(fileName, SOURCE_CONTAINER);

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
        blobExists();
        var sourceContainerName = "sourceContainer1";
        var targetContainerName = "targetContainer1";

        setupContainerConfig(sourceContainerName, targetContainerName, BULKSCAN);

        // valid file
        given(verifier.verifyZip(any(), any())).willReturn(ok());

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

        // valid file
        given(verifier.verifyZip(any(), any())).willReturn(ok());
        var fileName = "envelope1.zip";

        // when
        newBlobProcessor().process(fileName, sourceContainerName);

        // then
        verify(blobDispatcher, times(1))
            .dispatch(eq(fileName), aryEq(INTERNAL_ENVELOPE_CONTENT), eq(targetContainerName), eq(CRIME));
    }

    @Test
    void should_not_upload_to_crime_when_blob_is_not_zip() {
        // given
        blobExists(OffsetDateTime.now(), "not zip file's content".getBytes());

        var sourceContainerName = "sourceContainer1";
        var targetContainerName = "targetContainer1";

        setupContainerConfig(sourceContainerName, targetContainerName, CRIME);

        // valid file
        given(verifier.verifyZip(any(), any())).willReturn(ok());

        // when
        newBlobProcessor().process("envelope1.zip", sourceContainerName);

        // then
        verify(blobDispatcher, never()).dispatch(any(), any(), any(), any());
    }

    @Test
    void should_not_upload_to_crime_when_blob_does_not_contain_envelope_entry() {
        // given
        // blob contains signature file only
        blobExists(OffsetDateTime.now(), getBlobContent(Map.of("signature", "test".getBytes())));

        var sourceContainerName = "sourceContainer1";
        var targetContainerName = "targetContainer1";

        setupContainerConfig(sourceContainerName, targetContainerName, CRIME);

        // valid file
        given(verifier.verifyZip(any(), any())).willReturn(ok());

        // when
        newBlobProcessor().process("envelope1.zip", sourceContainerName);

        // then
        verify(blobDispatcher, never()).dispatch(any(), any(), any(), any());
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

    private void blobExists() {
        blobExists(OffsetDateTime.now(), BLOB_CONTENT);
    }

    private void blobExists(OffsetDateTime time, byte[] contentToDownload) {
        given(blobServiceClient.getBlobContainerClient(any())).willReturn(containerClient);
        given(containerClient.getBlobClient(any())).willReturn(blobClient);
        given(blobClient.getProperties()).willReturn(blobProperties);

        if (contentToDownload != null) {
            setupDownloadedBlobContent(contentToDownload);
        }

        given(blobProperties.getLastModified()).willReturn(time);
        given(envelopeService.findEnvelope(any(), any())).willReturn(Optional.empty());
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
            this.blobServiceClient,
            this.blobDispatcher,
            this.envelopeService,
            blobClient -> blobLeaseClient,
            this.verifier,
            this.serviceConfiguration
        );
    }
}
