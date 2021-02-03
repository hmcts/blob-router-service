package uk.gov.hmcts.reform.blobrouter.services.storage;

import com.azure.storage.blob.specialized.BlobInputStream;
import com.azure.storage.blob.specialized.BlockBlobClient;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

class ZipInputStreamCreatorTest {

    ZipInputStreamCreator zipInputStreamCreator = new ZipInputStreamCreator();


    @Test
    void should_return_zipInputStream() {
        BlobInputStream blobInputStream = mock(BlobInputStream.class);

        BlockBlobClient  blobClient = mock(BlockBlobClient.class); ;

        given(blobClient.openInputStream()).willReturn(blobInputStream);

        var stream = zipInputStreamCreator.getZipInputStream(blobClient);
        assertThat(stream).isNotNull();
    }
}