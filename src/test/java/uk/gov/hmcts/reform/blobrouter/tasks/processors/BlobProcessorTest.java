package uk.gov.hmcts.reform.blobrouter.tasks.processors;

import com.azure.storage.blob.BlobClient;
import com.azure.storage.blob.BlobContainerClient;
import com.azure.storage.blob.BlobServiceClient;
import com.azure.storage.blob.models.BlobProperties;
import com.azure.storage.blob.specialized.BlobLeaseClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.reform.blobrouter.data.EnvelopeRepository;
import uk.gov.hmcts.reform.blobrouter.services.BlobReadinessChecker;
import uk.gov.hmcts.reform.blobrouter.services.storage.BlobDispatcher;
import uk.gov.hmcts.reform.blobrouter.services.storage.LeaseClientProvider;

import java.time.OffsetDateTime;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class BlobProcessorTest {

    @Mock BlobReadinessChecker readinessChecker;
    @Mock BlobServiceClient blobServiceClient;
    @Mock BlobContainerClient containerClient;
    @Mock BlobClient blobClient;
    @Mock BlobProperties blobProperties;
    @Mock BlobDispatcher blobDispatcher;
    @Mock BlobLeaseClient blobLeaseClient;
    @Mock EnvelopeRepository envelopeRepo;

    BlobProcessor blobProcessor;

    @BeforeEach
    void setUp() {
        this.blobProcessor = new BlobProcessor(
            this.blobServiceClient,
            this.blobDispatcher,
            this.readinessChecker,
            this.envelopeRepo,
            blobClient -> blobLeaseClient
        );
    }

    @Test
    void should_not_process_file_if_it_is_not_ready_yet() {
        // given
        OffsetDateTime blobCreationTime = OffsetDateTime.now();

        given(blobServiceClient.getBlobContainerClient(any())).willReturn(containerClient);
        given(containerClient.getBlobClient(any())).willReturn(blobClient);
        given(blobClient.getProperties()).willReturn(blobProperties);
        given(blobProperties.getCreationTime()).willReturn(blobCreationTime);
        given(envelopeRepo.find(any(), any())).willReturn(Optional.empty());

        // file is NOT ready to be processed
        given(readinessChecker.isReady(any())).willReturn(false);

        // when
        blobProcessor.process("hello.zip", "my_container");

        // then
        verify(readinessChecker).isReady(eq(blobCreationTime.toInstant()));
        verify(blobDispatcher, never()).dispatch(any(), any(), any());
    }

    @Test
    void should_process_file_if_it_is_ready() {
        // given
        OffsetDateTime blobCreationTime = OffsetDateTime.now();

        given(blobServiceClient.getBlobContainerClient(any())).willReturn(containerClient);
        given(containerClient.getBlobClient(any())).willReturn(blobClient);
        given(blobClient.getProperties()).willReturn(blobProperties);
        given(blobProperties.getCreationTime()).willReturn(blobCreationTime);
        given(envelopeRepo.find(any(), any())).willReturn(Optional.empty());

        // file IS ready to be processed
        given(readinessChecker.isReady(any())).willReturn(true);

        // when
        blobProcessor.process("hello.zip", "my_container");

        // then
        verify(readinessChecker).isReady(eq(blobCreationTime.toInstant()));
        verify(blobDispatcher).dispatch(any(), any(), any());
    }
}
