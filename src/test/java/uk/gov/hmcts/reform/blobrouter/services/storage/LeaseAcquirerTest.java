package uk.gov.hmcts.reform.blobrouter.services.storage;

import com.azure.storage.blob.BlobClient;
import com.azure.storage.blob.models.BlobErrorCode;
import com.azure.storage.blob.models.BlobStorageException;
import com.azure.storage.blob.specialized.BlobLeaseClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.function.Consumer;

import static com.azure.storage.blob.models.BlobErrorCode.LEASE_ALREADY_PRESENT;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static uk.gov.hmcts.reform.blobrouter.services.storage.LeaseAcquirer.LEASE_DURATION_IN_SECONDS;

@ExtendWith(MockitoExtension.class)
@SuppressWarnings("unchecked")
class LeaseAcquirerTest {

    @Mock BlobClient blobClient;
    @Mock BlobLeaseClient leaseClient;
    @Mock BlobStorageException blobStorageException;
    @Mock BlobMetaDataHandler blobMetaDataHandler;

    private LeaseAcquirer leaseAcquirer;

    @BeforeEach
    void setUp() {
        leaseAcquirer = new LeaseAcquirer(blobClient -> leaseClient, blobMetaDataHandler);
    }

    @Test
    void should_run_provided_action_when_lease_was_acquired() {
        // given
        var onSuccess = mock(Consumer.class);
        var onFailure = mock(Consumer.class);
        String leaseId = "lease-id";
        given(leaseClient.acquireLease(LEASE_DURATION_IN_SECONDS)).willReturn(leaseId);
        given(blobMetaDataHandler.isBlobReadyToUse(blobClient, leaseId)).willReturn(true);

        // when
        leaseAcquirer.ifAcquiredOrElse(blobClient, onSuccess, onFailure, false);

        // then
        verify(onSuccess).accept(leaseId);
        verify(onFailure, never()).accept(any(BlobErrorCode.class));
    }

    @Test
    void should_run_provided_action_when_lease_was_not_acquired() {
        // given
        var onSuccess = mock(Consumer.class);
        var onFailure = mock(Consumer.class);

        doThrow(blobStorageException).when(leaseClient).acquireLease(anyInt());

        // when
        leaseAcquirer.ifAcquiredOrElse(blobClient, onSuccess, onFailure, false);

        // then
        verify(onSuccess, never()).accept(anyString());
        verify(onFailure).accept(null);
    }

    @Test
    void should_not_call_release_when_failed_to_process_blob() {
        // given
        doThrow(blobStorageException).when(leaseClient).acquireLease(anyInt());

        // when
        leaseAcquirer.ifAcquiredOrElse(blobClient, mock(Consumer.class), mock(Consumer.class), true);

        // then
        verify(leaseClient, never()).releaseLease();
    }

    @Test
    void should_call_release_when_successfully_processed_blob() {
        // given
        String leaseId = "lease-id";
        given(leaseClient.acquireLease(LEASE_DURATION_IN_SECONDS)).willReturn(leaseId);
        given(blobMetaDataHandler.isBlobReadyToUse(blobClient, leaseId)).willReturn(true);

        // when
        leaseAcquirer.ifAcquiredOrElse(blobClient, mock(Consumer.class), mock(Consumer.class), true);

        // then
        verify(leaseClient).releaseLease();
    }


    @Test
    void should_run_onFailure_when_metadata_lease_can_not_acquired() {
        // given
        var onSuccess = mock(Consumer.class);
        var onFailure = mock(Consumer.class);
        String leaseId = "lease-id";
        given(leaseClient.acquireLease(LEASE_DURATION_IN_SECONDS)).willReturn(leaseId);
        given(blobMetaDataHandler.isBlobReadyToUse(blobClient, leaseId)).willReturn(false);

        // when
        leaseAcquirer.ifAcquiredOrElse(blobClient, onSuccess, onFailure, false);

        // then
        verify(onSuccess, never()).accept(anyString());
        verify(onFailure).accept(any(BlobErrorCode.class));
        verify(leaseClient).releaseLease();

    }

    @Test
    void should_catch_exception_when_metadata_lease_clear_throw_exception() {
        // given
        var onSuccess = mock(Consumer.class);
        var onFailure = mock(Consumer.class);
        String leaseId = "lease-id";

        given(leaseClient.acquireLease(LEASE_DURATION_IN_SECONDS)).willReturn(leaseId);
        willThrow(blobStorageException).given(blobMetaDataHandler).isBlobReadyToUse(blobClient, leaseId);

        // when
        leaseAcquirer.ifAcquiredOrElse(blobClient, onSuccess, onFailure, false);

        // then
        verify(onSuccess, never()).accept(anyString());
        verify(onFailure).accept(LEASE_ALREADY_PRESENT);
        verify(leaseClient).releaseLease();
        verifyNoMoreInteractions(blobMetaDataHandler);
    }
}
