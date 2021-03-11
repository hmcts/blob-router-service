package uk.gov.hmcts.reform.blobrouter.tasks.processors;

import com.azure.storage.blob.BlobClient;
import com.azure.storage.blob.BlobContainerClient;
import com.azure.storage.blob.BlobContainerClientBuilder;
import com.azure.storage.blob.models.BlobItem;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import uk.gov.hmcts.reform.blobrouter.config.ServiceConfiguration;
import uk.gov.hmcts.reform.blobrouter.data.DbHelper;
import uk.gov.hmcts.reform.blobrouter.data.envelopes.EnvelopeRepository;
import uk.gov.hmcts.reform.blobrouter.services.BlobVerifier;
import uk.gov.hmcts.reform.blobrouter.services.EnvelopeService;
import uk.gov.hmcts.reform.blobrouter.services.storage.BlobContainerClientBuilderProvider;
import uk.gov.hmcts.reform.blobrouter.services.storage.BlobContainerClientProxy;
import uk.gov.hmcts.reform.blobrouter.services.storage.BlobDispatcher;
import uk.gov.hmcts.reform.blobrouter.services.storage.BlobMover;
import uk.gov.hmcts.reform.blobrouter.services.storage.SasTokenCache;
import uk.gov.hmcts.reform.blobrouter.util.BlobStorageBaseTest;

import java.io.ByteArrayInputStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static uk.gov.hmcts.reform.blobrouter.data.envelopes.Status.DISPATCHED;
import static uk.gov.hmcts.reform.blobrouter.testutils.DirectoryZipper.zipAndSignDir;

@ActiveProfiles("db-test")
@SpringBootTest
@ExtendWith(MockitoExtension.class)
class BlobProcessorTest extends BlobStorageBaseTest {

    BlobContainerClientProxy containerClientProvider;

    @Mock BlobContainerClientBuilderProvider blobContainerClientBuilderProvider;
    @Mock BlobContainerClientBuilder blobContainerClientBuilder;

    @Autowired EnvelopeService envelopeService;
    @Autowired EnvelopeRepository envelopeRepo;
    @Autowired ServiceConfiguration serviceConfiguration;
    @Autowired DbHelper dbHelper;
    @Autowired BlobMover blobMover;

    @BeforeEach
    void setUp() {
        containerClientProvider = new BlobContainerClientProxy(
            mock(BlobContainerClient.class),
            blobContainerClientBuilderProvider,
            mock(SasTokenCache.class)
        );
        dbHelper.deleteAll();
    }

    @Test
    void should_copy_file_from_source_to_target_container() throws Exception {
        // given
        var sourceContainer = "bulkscan";
        var targetContainer = "bulkscan-target";

        BlobContainerClient sourceContainerClient = createContainer(sourceContainer);
        BlobContainerClient targetContainerClient = createContainer(targetContainer);

        // configure Dispatcher to always send files to docker target container.
        given(blobContainerClientBuilderProvider.getBlobContainerClientBuilder())
            .willReturn(blobContainerClientBuilder);
        given(blobContainerClientBuilder.sasToken(any())).willReturn(blobContainerClientBuilder);
        given(blobContainerClientBuilder.containerName(any())).willReturn(blobContainerClientBuilder);
        given(blobContainerClientBuilder.buildClient()).willReturn(targetContainerClient);

        var dispatcher = new BlobDispatcher(containerClientProvider, blobMover);

        var blobProcessor =
            new BlobProcessor(
                dispatcher,
                envelopeService,
                new BlobVerifier("signing/test_public_key.der"),
                serviceConfiguration
            );

        var blobName = "hello.zip";
        byte[] bytes = zipAndSignDir("storage/valid", "signing/test_private_key.der");

        BlobClient blobClient = sourceContainerClient.getBlobClient(blobName);

        blobClient
            .getBlockBlobClient()
            .upload(new ByteArrayInputStream(bytes), bytes.length);

        // when
        blobProcessor.process(blobClient);

        // then
        assertThat(targetContainerClient.listBlobs())
            .extracting(BlobItem::getName)
            .as("File should be copied to target container")
            .containsExactly(blobName);

        assertThat(envelopeRepo.findLast(blobName, sourceContainer))
            .as("Envelope in the DB has been created")
            .hasValueSatisfying(envelope -> assertThat(envelope.getStatus()).isEqualTo(DISPATCHED));
    }
}
