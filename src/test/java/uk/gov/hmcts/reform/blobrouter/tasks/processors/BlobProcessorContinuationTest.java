package uk.gov.hmcts.reform.blobrouter.tasks.processors;

import com.azure.storage.blob.BlobClient;
import com.azure.storage.blob.specialized.BlobLeaseClient;
import org.junit.jupiter.api.BeforeEach;
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
import uk.gov.hmcts.reform.blobrouter.services.storage.LeaseAcquirer;

import java.io.OutputStream;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.will;
import static uk.gov.hmcts.reform.blobrouter.config.TargetStorageAccount.BULKSCAN;
import static uk.gov.hmcts.reform.blobrouter.config.TargetStorageAccount.CRIME;

@ExtendWith(MockitoExtension.class)
public class BlobProcessorContinuationTest {

    @Mock BlobDispatcher blobDispatcher;
    @Mock EnvelopeService envelopeService;
    @Mock LeaseAcquirer leaseAcquirer;
    @Mock BlobVerifier verifier;
    @Mock ServiceConfiguration serviceConfiguration;
    @Mock BlobLeaseClient blobLeaseClient;
    @Mock BlobClient blobClient;

    BlobProcessor blobProcessor;

    @BeforeEach
    void setUp() {
        blobProcessor = new BlobProcessor(
            blobDispatcher,
            envelopeService,
            leaseAcquirer,
            verifier,
            serviceConfiguration
        );

        given(serviceConfiguration.getStorageConfig())
            .willReturn(Map.of(
                "s1", cfg("s1", "t1", BULKSCAN),
                "s2", cfg("s2", "t2", CRIME)
            ));
    }

    @Test
    void should_not_create_new_envelope() {
        blobExists("hello.zip", "s1",);
        given(leaseAcquirer.acquireFor(any())).willReturn(Optional.of(blobLeaseClient));

        var id = UUID.randomUUID();
        blobProcessor.continueProcessing(id, blobClient);

    }

    private StorageConfigItem cfg(String source, String target, TargetStorageAccount targetAccount) {
        var cfg = new StorageConfigItem();
        cfg.setSourceContainer(source);
        cfg.setTargetContainer(target);
        cfg.setTargetStorageAccount(targetAccount);
        cfg.setEnabled(true);
        return cfg;
    }

    private void blobExists(String blobName, String containerName, byte[] contentToDownload) {
        given(blobClient.getBlobName()).willReturn(blobName);
        given(blobClient.getContainerName()).willReturn(containerName);

        if (contentToDownload != null) {
            setupDownloadedBlobContent(contentToDownload);
        }
    }

    private void setupDownloadedBlobContent(byte[] content) {
        will(invocation -> {
            var outputStream = (OutputStream) invocation.getArguments()[0];
            outputStream.write(content);
            return null;
        })
            .given(blobClient)
            .download(any());
    }
}
