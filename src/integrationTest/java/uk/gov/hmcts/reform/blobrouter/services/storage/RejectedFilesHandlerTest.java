package uk.gov.hmcts.reform.blobrouter.services.storage;

import com.azure.storage.blob.BlobContainerClient;
import com.azure.storage.blob.models.BlobItem;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import uk.gov.hmcts.reform.blobrouter.data.DbHelper;
import uk.gov.hmcts.reform.blobrouter.data.EnvelopeRepository;
import uk.gov.hmcts.reform.blobrouter.data.model.NewEnvelope;
import uk.gov.hmcts.reform.blobrouter.data.model.Status;
import uk.gov.hmcts.reform.blobrouter.util.BlobStorageBaseTest;

import static java.time.Instant.now;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.SoftAssertions.assertSoftly;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("db-test")
class RejectedFilesHandlerTest extends BlobStorageBaseTest {

    @Autowired EnvelopeRepository envelopeRepo;
    @Autowired DbHelper dbHelper;

    RejectedFilesHandler mover;

    @BeforeEach
    void setUp() {
        dbHelper.deleteAll();
        mover = new RejectedFilesHandler(storageClient, envelopeRepo);
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
}
