package uk.gov.hmcts.reform.blobrouter.tasks.processors;

import com.azure.core.test.TestBase;
import com.azure.storage.blob.BlobServiceClient;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import uk.gov.hmcts.reform.blobrouter.data.EnvelopeRepository;
import uk.gov.hmcts.reform.blobrouter.services.BlobReadinessChecker;
import uk.gov.hmcts.reform.blobrouter.services.storage.BlobDispatcher;
import uk.gov.hmcts.reform.blobrouter.services.storage.BlobServiceClientProvider;
import uk.gov.hmcts.reform.blobrouter.services.storage.LeaseClientProvider;
import uk.gov.hmcts.reform.blobrouter.util.StorageClientsHelper;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

@ActiveProfiles("db-test")
@SpringBootTest
class ContainerProcessorTest extends TestBase {

    private static final String CONTAINER_WITH_BLOBS = "bulkscan";
    private static final String CONTAINER_WITHOUT_BLOBS = "empty";

    @Autowired
    private BlobReadinessChecker readinessChecker;

    @Autowired
    private EnvelopeRepository envelopeRepository;

    @Autowired
    private LeaseClientProvider leaseClientProvider;

    private ContainerProcessor containerProcessor;

    @BeforeAll
    static void setUpTestMode() {
        StorageClientsHelper.setAzureTestMode();

        setupClass();
    }

    @Override
    protected void beforeTest() {
        BlobServiceClient storageClient = StorageClientsHelper.getStorageClient(interceptorManager);
        BlobServiceClientProvider blobServiceClientProvider = mock(BlobServiceClientProvider.class);
        given(blobServiceClientProvider.get(any(), any())).willReturn(storageClient);
        BlobDispatcher dispatcher = new BlobDispatcher(blobServiceClientProvider);
        BlobProcessor blobProcessor = new BlobProcessor(
            storageClient,
            dispatcher,
            readinessChecker,
            envelopeRepository,
            leaseClientProvider
        );
        containerProcessor = new ContainerProcessor(storageClient, blobProcessor);
    }

    @Test
    void should_find_blobs_and_process() {
        assertThatCode(() -> containerProcessor.process(CONTAINER_WITH_BLOBS))
            .doesNotThrowAnyException();
    }

    @Test
    void should_not_find_any_blobs() {
        assertThatCode(() -> containerProcessor.process(CONTAINER_WITHOUT_BLOBS))
            .doesNotThrowAnyException();
    }
}
