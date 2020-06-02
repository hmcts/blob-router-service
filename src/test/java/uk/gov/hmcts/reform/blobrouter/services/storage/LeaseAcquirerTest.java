package uk.gov.hmcts.reform.blobrouter.services.storage;

import com.azure.storage.blob.BlobClient;
import com.azure.storage.blob.models.BlobStorageException;
import com.azure.storage.blob.specialized.BlobLeaseClient;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static com.azure.storage.blob.models.BlobErrorCode.BLOB_NOT_FOUND;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.hmcts.reform.blobrouter.services.storage.LeaseAcquirer.LEASE_DURATION_IN_SECONDS;

@ExtendWith(MockitoExtension.class)
class LeaseAcquirerTest {

    @Mock BlobClient blobClient;
    @Mock BlobLeaseClient leaseClient;
    @Mock BlobStorageException blobStorageException;

    @Test
    void should_run_provided_action_when_lease_was_acquired() {
        // given
        var onSuccess = mock(Runnable.class);

        var leaseAcquirer = new LeaseAcquirer(blobClient -> leaseClient);

        // when
        leaseAcquirer.ifAcquiredOrElse(blobClient, onSuccess);

        // then
        verify(onSuccess).run();

        verify(leaseClient).releaseLease();
    }

    @Test
    void should_run_provided_action_when_lease_was_not_acquired() {
        // given
        var onSuccess = mock(Runnable.class);

        doThrow(blobStorageException).when(leaseClient).acquireLease(anyInt());

        var leaseAcquirer = new LeaseAcquirer(blobClient -> leaseClient);

        // when
        leaseAcquirer.ifAcquiredOrElse(blobClient, onSuccess);

        // then
        verify(onSuccess, never()).run();

        verify(leaseClient, never()).releaseLease();
    }

    @Test
    void should_handle_error_when_releasing_lease() {
        // given
        var onSuccess = mock(Runnable.class);

        doThrow(blobStorageException).when(leaseClient).releaseLease();

        var leaseAcquirer = new LeaseAcquirer(blobClient -> leaseClient);

        // when
        var exc = catchThrowable(() -> leaseAcquirer.ifAcquiredOrElse(blobClient, onSuccess));

        // then
        assertThat(exc).isNull();
        verify(onSuccess).run();

        verify(leaseClient).releaseLease();
    }

    @Test
    void should_run_custom_function_when_blob_not_found() {
        // given
        var onSuccess = mock(Runnable.class);
        var onBlobNotFound = mock(Runnable.class);
        doThrow(blobStorageException).when(leaseClient).acquireLease(LEASE_DURATION_IN_SECONDS);
        when(blobStorageException.getErrorCode()).thenReturn(BLOB_NOT_FOUND);
        var leaseAcquirer = new LeaseAcquirer(blobClient1 -> leaseClient);

        // when
        leaseAcquirer.ifAcquiredOrElse(blobClient, onSuccess, onBlobNotFound);

        // then
        verify(onSuccess, never()).run();
        verify(onBlobNotFound).run();
        verify(leaseClient, never()).releaseLease();
    }
}
