package uk.gov.hmcts.reform.blobrouter.services.storage;

import com.azure.storage.blob.BlobContainerClient;
import com.azure.storage.blob.models.BlobItem;
import com.azure.storage.blob.models.BlobListDetails;
import com.azure.storage.blob.models.ListBlobsOptions;
import com.google.common.io.Resources;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import uk.gov.hmcts.reform.blobrouter.util.BlobStorageBaseTest;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.Base64;
import java.util.List;

import static com.google.common.io.Resources.getResource;
import static java.util.stream.Collectors.toList;
import static org.assertj.core.api.SoftAssertions.assertSoftly;

@SuppressWarnings("unchecked")
class BlobMoverTest extends BlobStorageBaseTest {

    BlobMover mover;

    @BeforeEach
    void setUp() {
        mover = new BlobMover(storageClient, 260);
    }

    @AfterEach
    void tearDown() {
        this.deleteAllContainers();
    }

    @Test
    @SuppressWarnings("checkstyle:variabledeclarationusagedistance")
    void should_move_file_to_rejected_container() {
        // given
        BlobContainerClient normalContainer = createContainer("sample-container");
        BlobContainerClient rejectedContainer = createContainer("sample-container-rejected");

        var blobName = "hello.zip";

        normalContainer
            .getBlobClient(blobName)
            .uploadFromFile("src/integrationTest/resources/storage/test1.zip");

        // when
        mover.moveToRejectedContainer(blobName, "sample-container");

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

        // when
        mover.moveToRejectedContainer(blobName, "hello");


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
                .containsExactly(
                    blobName,
                    blobName
                );

            softly
                .assertThat(normalContainer.listBlobs())
                .as("File should be removed from source container")
                .isEmpty();
        });
    }

    @Test
    void upload_by_chunks_when_uploads() throws IOException {

        BlobContainerClient targetContainer = createContainer("sample-container-2");
        var targetBlobName = "copy_hello.zip";

        // size 264 byte
        byte[] content = Resources.toByteArray(
            getResource("storage/test1.zip")
        );

        var expectedBlockIdList = List.of(
            Base64.getEncoder().encodeToString("0000001".getBytes()),
            Base64.getEncoder().encodeToString("0000002".getBytes())
        );

        var blockBlobClient = targetContainer.getBlobClient(targetBlobName).getBlockBlobClient();

        List<String> blockIdList = mover
            .uploadWithChunks(blockBlobClient, new ByteArrayInputStream(content));

        // then
        assertSoftly(softly -> {
            softly
                .assertThat(expectedBlockIdList)
                .containsExactlyElementsOf(blockIdList);

            softly
                .assertThat(targetContainer.listBlobs().stream().map(BlobItem::getName))
                .as("File should be moved to target container")
                .containsExactly(targetBlobName);
        });
    }
}
