package uk.gov.hmcts.reform.blobrouter.services.storage;

import com.azure.core.http.rest.PagedIterable;
import com.azure.storage.blob.BlobContainerClient;
import com.azure.storage.blob.BlobServiceClient;
import com.azure.storage.blob.models.BlobItem;
import com.azure.storage.blob.models.BlobItemProperties;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.reform.blobrouter.config.ServiceConfiguration;
import uk.gov.hmcts.reform.blobrouter.model.out.BlobInfo;

import java.time.OffsetDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Stream;

import static java.time.OffsetDateTime.now;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

@ExtendWith(MockitoExtension.class)
class StaleBlobFinderTest {

    @Mock
    private BlobServiceClient storageClient;

    @Mock
    private ServiceConfiguration serviceConfiguration;

    @InjectMocks
    private StaleBlobFinder staleBlobFinder;

    private final OffsetDateTime expectedCreationTime = now().minus(3, ChronoUnit.HOURS);

    @Test
    void should_find_stale_blobs() {
        given(serviceConfiguration.getSourceContainers())
            .willReturn(Arrays.asList("bulkscan", "cmc", "sscs"));

        var bulkscanBlobClient = mock(BlobContainerClient.class);
        var cmcBlobClient = mock(BlobContainerClient.class);
        var sscsBlobClient = mock(BlobContainerClient.class);
        given(storageClient.getBlobContainerClient("bulkscan")).willReturn(bulkscanBlobClient);
        given(storageClient.getBlobContainerClient("cmc")).willReturn(cmcBlobClient);
        given(storageClient.getBlobContainerClient("sscs")).willReturn(sscsBlobClient);

        mockStorageList(
            bulkscanBlobClient,
            Stream.of(
                blob("bulk_scan_file_new", false),
                blob("bulk_scan_file_stale", true)
            )
        );

        mockStorageList(
            cmcBlobClient,
            Stream.of(
                blob("cmc_scan_file_new_1", false),
                blob("cmc_scan_file_new_2", false)
            )
        );

        mockStorageList(sscsBlobClient, Stream.empty());

        List<BlobInfo> blobInfos = staleBlobFinder.findStaleBlobs(120);

        assertThat(blobInfos.size()).isEqualTo(1);
        assertThat(blobInfos.get(0).container).isEqualTo("bulkscan");
        assertThat(blobInfos.get(0).fileName).isEqualTo("bulk_scan_file_stale");
        assertThat(blobInfos.get(0).createdAt).isEqualTo(expectedCreationTime.toInstant());
    }

    @SuppressWarnings("unchecked")
    private void mockStorageList(BlobContainerClient blobClient, Stream<BlobItem> streamOfBlobItem) {
        PagedIterable<BlobItem> listBlobsResult = mock(PagedIterable.class);
        given(blobClient.listBlobs()).willReturn(listBlobsResult);
        given(listBlobsResult.stream()).willReturn(streamOfBlobItem);

    }

    private BlobItem blob(String name, boolean staleFile) {
        var blobItem = mock(BlobItem.class);
        var properties = mock(BlobItemProperties.class);

        given(blobItem.getProperties()).willReturn(properties);
        if (staleFile) {
            given(properties.getCreationTime()).willReturn(expectedCreationTime);
            given(blobItem.getName()).willReturn(name);
        } else {
            given(properties.getCreationTime()).willReturn(now());
        }
        return blobItem;
    }
}
