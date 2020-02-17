package uk.gov.hmcts.reform.blobrouter.services.storage;

import com.azure.storage.blob.BlobContainerClient;
import com.azure.storage.blob.models.BlobItem;
import com.azure.storage.blob.models.BlobListDetails;
import com.azure.storage.blob.models.ListBlobsOptions;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import uk.gov.hmcts.reform.blobrouter.data.DbHelper;
import uk.gov.hmcts.reform.blobrouter.data.EnvelopeRepository;
import uk.gov.hmcts.reform.blobrouter.data.model.NewEnvelope;
import uk.gov.hmcts.reform.blobrouter.data.model.Status;
import uk.gov.hmcts.reform.blobrouter.services.EnvelopeService;
import uk.gov.hmcts.reform.blobrouter.util.BlobStorageBaseTest;

import java.util.List;

import static java.time.Instant.now;
import static java.util.stream.Collectors.toList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.SoftAssertions.assertSoftly;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("db-test")
class RejectedFilesHandlerTest extends BlobStorageBaseTest {

    @Autowired EnvelopeService envelopeService;
    @Autowired EnvelopeRepository envelopeRepo;
    @Autowired DbHelper dbHelper;

    RejectedFilesHandler mover;

    @BeforeEach
    void setUp() {
        dbHelper.deleteAll();
        mover = new RejectedFilesHandler(envelopeService, new BlobRejecter(storageClient));
    }

    @AfterEach
    void tearDown() {
        this.deleteAllContainers();
    }

    @Test
    @SuppressWarnings("checkstyle:variabledeclarationusagedistance")
    void should_handle_rejected_files() {
        // given
        BlobContainerClient normalContainer = createContainer("sample-container");
        BlobContainerClient rejectedContainer = createContainer("sample-container-rejected");

        var blobName = "hello.zip";

        normalContainer
            .getBlobClient(blobName)
            .uploadFromFile("src/integrationTest/resources/storage/test1.zip");

        envelopeRepo.insert(new NewEnvelope("sample-container", "hello.zip", now(), now(), Status.REJECTED));

        // when
        mover.handle();

        // then
        assertSoftly(softly -> {
            softly
                .assertThat(normalContainer.listBlobs())
                .as("File should be removed from normal container")
                .hasSize(0);

            softly
                .assertThat(rejectedContainer.listBlobs().stream().map(BlobItem::getName))
                .as("File should be moved to rejected container")
                .containsExactly("hello.zip");

            softly
                .assertThat(envelopeRepo.find(blobName, "sample-container"))
                .as("Envelope in the DB should be updated")
                .hasValueSatisfying(
                    envelope -> assertThat(envelope.isDeleted).isTrue()
                );
        });
    }

    @Test
    void should_create_snapshot_of_file_if_it_already_exists_in_the_rejected_container() {
        // given
        BlobContainerClient normalContainer = createContainer("hello");
        BlobContainerClient rejectedContainer = createContainer("hello-rejected");

        var blobName = "foo.zip";

        normalContainer
            .getBlobClient(blobName)
            .uploadFromFile("src/integrationTest/resources/storage/test1.zip");

        rejectedContainer
            .getBlobClient(blobName)
            .uploadFromFile("src/integrationTest/resources/storage/test1.zip");

        envelopeRepo.insert(new NewEnvelope("hello", blobName, now(), now(), Status.REJECTED));

        // when
        mover.handle();


        List<BlobItem> blobsAndSnapshots =
            rejectedContainer
                .listBlobs(new ListBlobsOptions().setDetails(new BlobListDetails().setRetrieveSnapshots(true)), null)
                .stream().collect(toList());

        // then
        assertSoftly(softly -> {
            softly
                .assertThat(blobsAndSnapshots)
                .extracting(BlobItem::getName)
                .as("Snapshot should be created")
                .containsExactly(blobName, blobName);

            softly
                .assertThat(normalContainer.listBlobs())
                .as("File should be removed from source container")
                .hasSize(0);

            softly
                .assertThat(envelopeRepo.find(blobName, "hello"))
                .as("Envelope in the DB should be updated")
                .hasValueSatisfying(
                    envelope -> assertThat(envelope.isDeleted).isTrue()
                );
        });
    }
}
