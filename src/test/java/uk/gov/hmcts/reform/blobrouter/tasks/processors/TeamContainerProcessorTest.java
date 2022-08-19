package uk.gov.hmcts.reform.blobrouter.tasks.processors;

import com.azure.storage.blob.BlobServiceClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import uk.gov.hmcts.reform.blobrouter.services.BlobVerifier;
import uk.gov.hmcts.reform.blobrouter.services.storage.LeaseAcquirer;

import static java.util.Collections.emptyList;
import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

class TeamContainerProcessorTest {

    @Mock
    BlobServiceClient storageClient;
    @Mock
    LeaseAcquirer leaseAcquirer;
    @Mock
    BlobVerifier blobVerifier;

    TeamContainerProcessor teamContainerProcessor;

    @BeforeEach
    void setUp() {
        teamContainerProcessor = new TeamContainerProcessor(storageClient, leaseAcquirer, blobVerifier);
    }

    @Test
    void should_return_an_empty_list() {
        var envelopes = teamContainerProcessor.leaseAndGetEnvelopes("nfd");

        assertThat(envelopes).isEqualTo(emptyList());
    }
}
