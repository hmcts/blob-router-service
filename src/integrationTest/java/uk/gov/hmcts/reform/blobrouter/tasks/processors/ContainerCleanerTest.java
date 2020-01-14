package uk.gov.hmcts.reform.blobrouter.tasks.processors;

import com.azure.core.test.TestBase;
import com.azure.storage.blob.BlobServiceClient;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.test.context.ActiveProfiles;
import org.testcontainers.containers.DockerComposeContainer;
import uk.gov.hmcts.reform.blobrouter.data.DbHelper;
import uk.gov.hmcts.reform.blobrouter.data.EnvelopeRepositoryImpl;
import uk.gov.hmcts.reform.blobrouter.data.model.NewEnvelope;
import uk.gov.hmcts.reform.blobrouter.util.StorageClientsHelper;

import java.time.Instant;
import java.util.UUID;

import static java.lang.String.format;
import static org.assertj.core.api.Assertions.assertThat;
import static uk.gov.hmcts.reform.blobrouter.data.model.Status.DISPATCHED;

@ActiveProfiles("db-test")
@SpringBootTest
@ExtendWith(OutputCaptureExtension.class)
class ContainerCleanerTest extends TestBase {

    private static final String CONTAINER_NAME = "bulkscan";

    private ContainerCleaner containerCleaner;

    @Autowired
    private EnvelopeRepositoryImpl envelopeRepository;

    @Autowired
    private DbHelper dbHelper;

    private static DockerComposeContainer dockerComposeContainer;

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
        BlobServiceClient storageClient = StorageClientsHelper.getStorageSyncClient(interceptorManager);
        containerCleaner = new ContainerCleaner(storageClient, envelopeRepository);
    }

    @Test
    void should_find_blobs_and_delete(CapturedOutput output) throws Exception {
        // given
        createEnvelope("file1.zip");
        createEnvelope("file2.zip");
        Thread.sleep(1000); // need to wait for subscriber to be notified

        // when
        containerCleaner.process(CONTAINER_NAME);

        // then
        assertOutputCapture(output, "file1.zip", "file2.zip");
    }

    @Test
    void should_not_find_any_blobs(CapturedOutput output) throws Exception {
        // given

        // when
        containerCleaner.process(CONTAINER_NAME);
        Thread.sleep(1000); // need to wait for subscriber to be notified

        // then
        assertOutputCapture(output);
    }

    private UUID createEnvelope(String fileName) {
        NewEnvelope envelope = new NewEnvelope(
            CONTAINER_NAME,
            fileName,
            Instant.now(),
            Instant.now(),
            DISPATCHED
        );
        return envelopeRepository.insert(envelope);
    }

    private void assertOutputCapture(CapturedOutput output, String... fileNames) {
        String simpleLoggerName = ContainerCleaner.class.getSimpleName();
        assertThat(output).contains(
            simpleLoggerName + " Started deleting dispatched blobs from container " + CONTAINER_NAME
        );

        if (fileNames == null || fileNames.length == 0) {
            assertThat(output).doesNotContain("Deleted dispatched blob");
        } else {
            for (String fileName : fileNames) {
                assertThat(output).contains(
                    format(
                        "%s Deleted dispatched blob %s from container %s",
                        simpleLoggerName,
                        fileName,
                        CONTAINER_NAME
                    )
                );
            }
        }

        assertThat(output).contains(
            simpleLoggerName + " Finished deleting dispatched blobs from container " + CONTAINER_NAME
        );
    }
}
