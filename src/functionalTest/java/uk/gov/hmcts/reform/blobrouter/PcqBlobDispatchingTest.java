package uk.gov.hmcts.reform.blobrouter;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import uk.gov.hmcts.reform.blobrouter.data.envelopes.Status;

import java.util.concurrent.TimeUnit;

import static com.jayway.awaitility.Awaitility.await;
import static java.util.Arrays.asList;
import static uk.gov.hmcts.reform.blobrouter.envelope.ZipFileHelper.createZipArchive;
import static uk.gov.hmcts.reform.blobrouter.storage.StorageHelper.blobExists;
import static uk.gov.hmcts.reform.blobrouter.storage.StorageHelper.uploadFile;

public class PcqBlobDispatchingTest extends FunctionalTestBase {

    @Override
    @BeforeEach
    protected void setUp() {
        super.setUp();
    }

    /*
    Note: to run this locally, you need to generate a SAS token by connecting to Azure Storage Explorers pcqlocal
    account. Then right click and generate a SAS Token.
    Finally, within SasTokenCache.java, replace:
        final String sasToken = pcqClient.getSasToken(authTokenGenerator.generate()).sasToken;
    With:
        final String sasToken =
            <The sas token retrieved; exluding the BlobEndpoint, QueueEndpoint and TableEndpoint structure elements>
            I.e:
            sv=<value>&ss=<value>&srt=<value>&st=<value>se=<value>sp=<value> (without ; in the string at the end)
     */
    @Test
    void should_move_extracted_pcq_envelope_to_pcq_storage() throws Exception {
        // upload pcq file with unique name
        String fileName = randomFileName();

        byte[] wrappingZipContent = createZipArchive(
            asList("test-data/envelope/envelope.zip", "test-data/envelope/signature")
        );

        // when
        uploadFile(blobRouterStorageClient, config.pcqSourceContainer, fileName, wrappingZipContent);

        // then
        await("Wait for the blob to disappear from source container")
            .atMost(2, TimeUnit.MINUTES)
            .until(
                () -> !blobExists(blobRouterStorageClient, config.pcqSourceContainer, fileName)
            );

        assertFileInfoIsStored(fileName, config.pcqSourceContainer, Status.DISPATCHED, true);
    }
}
