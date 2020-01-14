package uk.gov.hmcts.reform.blobrouter.tasks.processors;

import com.azure.core.test.TestBase;
import com.azure.storage.blob.BlobServiceClient;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import uk.gov.hmcts.reform.blobrouter.data.DbHelper;
import uk.gov.hmcts.reform.blobrouter.data.EnvelopeRepositoryImpl;
import uk.gov.hmcts.reform.blobrouter.data.model.Envelope;
import uk.gov.hmcts.reform.blobrouter.data.model.NewEnvelope;
import uk.gov.hmcts.reform.blobrouter.data.model.Status;
import uk.gov.hmcts.reform.blobrouter.util.StorageClientsHelper;

import java.time.Instant;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static uk.gov.hmcts.reform.blobrouter.data.model.Status.DISPATCHED;
import static uk.gov.hmcts.reform.blobrouter.data.model.Status.REJECTED;

@ActiveProfiles("db-test")
@SpringBootTest
class ContainerCleanerTest extends TestBase {

    private static final String CONTAINER_NAME = "bulkscan";

    private ContainerCleaner containerCleaner;

    @Autowired
    private EnvelopeRepositoryImpl envelopeRepository;

    @Autowired
    private DbHelper dbHelper;

    @BeforeAll
    static void setUpTestMode() {
        StorageClientsHelper.setAzureTestMode();

        setupClass();
    }

    @BeforeEach
    void setUp() {
        dbHelper.deleteAll();
    }

    @Override
    protected void beforeTest() {
        BlobServiceClient storageClient = StorageClientsHelper.getStorageClient(interceptorManager);
        containerCleaner = new ContainerCleaner(storageClient, envelopeRepository);
    }

    @Test
    void should_not_find_any_blobs() {
        assertThatCode(() -> containerCleaner.process(CONTAINER_NAME)).doesNotThrowAnyException();
    }

    @Test
    void should_find_blobs_and_delete() {
        // given
        String[] dispatchedFileNames = new String[]{"file1.zip", "file3.zip"};
        createEnvelopes(DISPATCHED, dispatchedFileNames);

        // when
        assertThatCode(() -> containerCleaner.process(CONTAINER_NAME)).doesNotThrowAnyException();

        // then
        assertFilesIsDeleteState(true, dispatchedFileNames);
    }

    @Test
    void should_delete_only_dispatched_blobs() {
        // given
        String[] dispatchedFileNames = new String[]{"file1.zip", "file3.zip"};
        String[] rejectedFileNames = new String[]{"file2.zip", "file4.zip"};
        createEnvelopes(DISPATCHED, dispatchedFileNames);
        createEnvelopes(REJECTED, rejectedFileNames);

        // when
        assertThatCode(() -> containerCleaner.process(CONTAINER_NAME)).doesNotThrowAnyException();

        // then
        assertFilesIsDeleteState(true, dispatchedFileNames);
        assertFilesIsDeleteState(false, rejectedFileNames);
    }

    @Test
    void should_handle_non_exiting_file() {
        // given
        String[] dispatchedFileNames = new String[]{"causes_404.zip"};
        createEnvelopes(DISPATCHED, dispatchedFileNames);

        // when
        assertThatCode(() -> containerCleaner.process(CONTAINER_NAME)).doesNotThrowAnyException();

        // then
        assertFilesIsDeleteState(true, dispatchedFileNames);
    }

    @Test
    void should_handle_server_error() {
        // given
        String[] dispatchedFileNames = new String[]{"causes_500.zip"};
        createEnvelopes(DISPATCHED, dispatchedFileNames);

        // when
        assertThatCode(() -> containerCleaner.process(CONTAINER_NAME)).doesNotThrowAnyException();
    }

    private void createEnvelopes(Status status, String... fileNames) {
        for (String fileName : fileNames) {
            NewEnvelope envelope = new NewEnvelope(
                CONTAINER_NAME,
                fileName,
                Instant.now(),
                Instant.now(),
                status
            );
            envelopeRepository.insert(envelope);
        }
    }

    private void assertFilesIsDeleteState(boolean isDeleted, String... fileNames) {
        for (String fileName : fileNames) {
            final Optional<Envelope> envelope = envelopeRepository.find(fileName, CONTAINER_NAME);
            assertThat(envelope.isPresent()).isTrue();
            assertThat(envelope.get().isDeleted).isEqualTo(isDeleted);
        }
    }
}
