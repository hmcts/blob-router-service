package uk.gov.hmcts.reform.blobrouter.services.storage;

import com.azure.storage.blob.BlobClient;
import com.azure.storage.blob.BlobContainerClient;
import com.azure.storage.blob.BlobServiceClient;
import com.azure.storage.blob.BlobServiceClientBuilder;
import com.azure.storage.blob.models.BlobItem;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.test.context.ActiveProfiles;
import org.testcontainers.containers.DockerComposeContainer;
import uk.gov.hmcts.reform.blobrouter.data.DbHelper;
import uk.gov.hmcts.reform.blobrouter.data.EnvelopeRepositoryImpl;
import uk.gov.hmcts.reform.blobrouter.data.model.NewEnvelope;
import uk.gov.hmcts.reform.blobrouter.data.model.Status;
import uk.gov.hmcts.reform.blobrouter.tasks.processors.ContainerCleaner;
import uk.gov.hmcts.reform.blobrouter.util.StorageClientsHelper;

import java.io.File;
import java.time.Instant;

import static com.azure.core.test.TestBase.setupClass;
import static org.apache.commons.lang.ArrayUtils.addAll;
import static org.assertj.core.api.Assertions.assertThat;
import static uk.gov.hmcts.reform.blobrouter.data.model.Status.DISPATCHED;
import static uk.gov.hmcts.reform.blobrouter.data.model.Status.REJECTED;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("db-test")
@ExtendWith(OutputCaptureExtension.class)
public class ContainerCleanerIntTest {
    private static final String CONTAINER_NAME = "bulkscan";

    private static final String TEST_1 = "test1.zip";
    private static final String TEST_2 = "test2.zip";
    private static final String TEST_3 = "test3.zip";
    private static final String TEST_4 = "test4.zip";

    private static DockerComposeContainer dockerComposeContainer;

    private ContainerCleaner containerCleaner;

    private BlobServiceClient storageClient;

    private BlobContainerClient containerClient;

    @Autowired
    private EnvelopeRepositoryImpl envelopeRepository;

    @Autowired
    private DbHelper dbHelper;

    @BeforeAll
    static void setUpTestMode() {
        StorageClientsHelper.setAzureTestMode();
        setupClass();

        dockerComposeContainer =
            new DockerComposeContainer(new File("src/integrationTest/resources/docker-compose.yml"))
                .withExposedService("azure-storage", 10000);
        dockerComposeContainer.start();
    }

    @AfterAll
    static void tearDownTestMode() {
        dockerComposeContainer.stop();
    }

    @BeforeEach
    void setUp() {
        dbHelper.deleteAll();

        storageClient =
            new BlobServiceClientBuilder()
                .connectionString("UseDevelopmentStorage=true")
                .buildClient();
        containerClient = storageClient.createBlobContainer(CONTAINER_NAME);

        containerCleaner = new ContainerCleaner(storageClient, envelopeRepository);
    }

    @AfterEach
    void tearDown() {
        storageClient.deleteBlobContainer(CONTAINER_NAME);
    }

    @Test
    public void should_find_blobs_and_delete() {
        // given
        String[] dispatchedFileNames = new String[]{TEST_1, TEST_3};

        uploadFilesToStorage(dispatchedFileNames);

        // ensure files have been uploaded
        assertThat(containerClient.listBlobs()).hasSize(dispatchedFileNames.length);
        assertThat(containerClient.listBlobs().stream().map(BlobItem::getName))
            .containsExactlyInAnyOrder(dispatchedFileNames);

        createEnvelopes(DISPATCHED, dispatchedFileNames);

        // when
        containerCleaner.process(CONTAINER_NAME);

        // then
        assertThat(containerClient.listBlobs()).isEmpty();
        assertFilesIsDeleteState(true, dispatchedFileNames);
    }

    @Test
    public void should_delete_only_dispatched_blobs() {
        // given
        String[] dispatchedFileNames = new String[]{TEST_1, TEST_3};
        String[] rejectedFileNames = new String[]{TEST_2, TEST_4};

        uploadFilesToStorage(dispatchedFileNames);
        uploadFilesToStorage(rejectedFileNames);

        // ensure files have been uploaded
        assertThat(containerClient.listBlobs()).hasSize(dispatchedFileNames.length + rejectedFileNames.length);
        assertThat(containerClient.listBlobs().stream().map(BlobItem::getName))
            .containsExactlyInAnyOrder((String[]) addAll(dispatchedFileNames, rejectedFileNames));

        createEnvelopes(DISPATCHED, dispatchedFileNames);
        createEnvelopes(REJECTED, rejectedFileNames);

        // when
        containerCleaner.process(CONTAINER_NAME);

        // then
        assertThat(containerClient.listBlobs()).hasSize(rejectedFileNames.length);
        assertThat(containerClient.listBlobs().stream().map(BlobItem::getName))
            .containsExactlyInAnyOrder(rejectedFileNames);
        assertFilesIsDeleteState(true, dispatchedFileNames);
        assertFilesIsDeleteState(false, rejectedFileNames);
    }

    @Test
    public void should_handle_non_existing_file() {
        // given
        // 2 envelopes in the database
        String[] dispatchedFileNames = new String[]{TEST_1, TEST_3};
        createEnvelopes(DISPATCHED, dispatchedFileNames);

        // but only 1 file uploaded
        uploadFilesToStorage(TEST_1);

        // ensure file have been uploaded
        assertThat(containerClient.listBlobs()).hasSize(1);
        assertThat(containerClient.listBlobs().stream().map(BlobItem::getName))
            .containsExactlyInAnyOrder(TEST_1);

        // when
        containerCleaner.process(CONTAINER_NAME);

        // then
        assertThat(containerClient.listBlobs()).isEmpty();
        assertFilesIsDeleteState(true, dispatchedFileNames);
    }

    private void uploadFilesToStorage(String... fileNames) {
        for (String fileName: fileNames) {
            BlobClient blobClient = containerClient.getBlobClient(fileName);
            blobClient.uploadFromFile("src/integrationTest/resources/storage/" + fileName);
        }
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
            assertThat(envelopeRepository.find(fileName, CONTAINER_NAME).isPresent()).isTrue();
            assertThat(envelopeRepository.find(fileName, CONTAINER_NAME).get().isDeleted).isEqualTo(isDeleted);
        }
    }
}
