package uk.gov.hmcts.reform.blobrouter.tasks.processors;

import com.azure.storage.blob.BlobContainerClient;
import com.azure.storage.blob.models.BlobItem;
import com.azure.storage.blob.specialized.BlobLeaseClientBuilder;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import uk.gov.hmcts.reform.blobrouter.config.ServiceConfiguration;
import uk.gov.hmcts.reform.blobrouter.data.EnvelopeRepository;
import uk.gov.hmcts.reform.blobrouter.services.BlobReadinessChecker;
import uk.gov.hmcts.reform.blobrouter.services.BlobSignatureVerifier;
import uk.gov.hmcts.reform.blobrouter.services.storage.BlobContainerClientProvider;
import uk.gov.hmcts.reform.blobrouter.services.storage.BlobDispatcher;
import uk.gov.hmcts.reform.blobrouter.util.BlobStorageBaseTest;

import java.io.ByteArrayInputStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static uk.gov.hmcts.reform.blobrouter.data.model.Status.DISPATCHED;
import static uk.gov.hmcts.reform.blobrouter.util.DirectoryZipper.zipAndSignDir;

@ActiveProfiles("db-test")
@SpringBootTest
@ExtendWith(MockitoExtension.class)
class BlobProcessorTest extends BlobStorageBaseTest {

    @Mock BlobContainerClientProvider containerClientProvider;

    @Autowired EnvelopeRepository envelopeRepo;
    @Autowired BlobReadinessChecker readinessChecker;
    @Autowired ServiceConfiguration serviceConfiguration;

    @Test
    void should_copy_file_from_source_to_target_container() throws Exception {
        // given
        var sourceContainer = "bulkscan";
        var targetContainer = "bulkscan-target";

        BlobContainerClient sourceContainerClient = createContainer(sourceContainer);
        BlobContainerClient targetContainerClient = createContainer(targetContainer);

        // configure Dispatcher to always send files to docker target container.
        given(containerClientProvider.get(any(), any())).willReturn(targetContainerClient);
        var dispatcher = new BlobDispatcher(containerClientProvider);

        var blobProcessor =
            new BlobProcessor(
                storageClient,
                dispatcher,
                readinessChecker,
                envelopeRepo,
                blobClient -> new BlobLeaseClientBuilder().blobClient(blobClient).buildClient(),
                new BlobSignatureVerifier("signing/test_public_key.der"),
                serviceConfiguration
            );

        var blobName = "hello.zip";
        byte[] bytes = zipAndSignDir("storage/valid", "signing/test_private_key.der");

        sourceContainerClient
            .getBlobClient(blobName)
            .getBlockBlobClient()
            .upload(new ByteArrayInputStream(bytes), bytes.length);

        // when
        blobProcessor.process(blobName, sourceContainer);

        // then
        assertThat(targetContainerClient.listBlobs())
            .extracting(BlobItem::getName)
            .as("File should be copied to target container")
            .containsExactly(blobName);

        assertThat(envelopeRepo.find(blobName, sourceContainer))
            .as("Envelope in the DB has been created")
            .hasValueSatisfying(envelope -> assertThat(envelope.status).isEqualTo(DISPATCHED));
    }
}
