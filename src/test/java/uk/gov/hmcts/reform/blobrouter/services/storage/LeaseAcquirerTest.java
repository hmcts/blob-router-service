package uk.gov.hmcts.reform.blobrouter.services.storage;

import com.azure.storage.blob.BlobClient;
import com.azure.storage.blob.models.BlobErrorCode;
import com.azure.storage.blob.models.BlobStorageException;
import com.azure.storage.blob.specialized.BlobLeaseClient;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class LeaseAcquirerTest {

    @Mock BlobClient blobClient;
    @Mock BlobLeaseClient leaseClient;
    @Mock BlobStorageException blobStorageException;

    @Test
    void should_return_lease_client_if_acquiring_lease_succeeded() {
        // given
        var leaseAcquirer = new LeaseAcquirer(blobClient -> leaseClient);

        // when
        Optional<BlobLeaseClient> result = leaseAcquirer.acquireFor(blobClient);

        // then
        assertThat(result).contains(leaseClient);
        verify(leaseClient).acquireLease(LeaseAcquirer.LEASE_DURATION_IN_SECONDS);
    }

    @Test
    void should_return_empty_optional_if_lease_for_given_blob_has_already_been_acquired() {
        // given
        given(blobStorageException.getErrorCode()).willReturn(BlobErrorCode.LEASE_ALREADY_PRESENT);
        doThrow(blobStorageException).when(leaseClient).acquireLease(anyInt());

        var leaseAcquirer = new LeaseAcquirer(blobClient -> leaseClient);

        // when
        Optional<BlobLeaseClient> result = leaseAcquirer.acquireFor(blobClient);

        // then
        assertThat(result).isEmpty();
    }

    @Test
    void should_throw_exception_if_lease_cannot_be_acquired_for_unknown_reason() {
        // given
        given(blobStorageException.getErrorCode()).willReturn(BlobErrorCode.INTERNAL_ERROR);
        doThrow(blobStorageException).when(leaseClient).acquireLease(anyInt());

        var leaseAcquirer = new LeaseAcquirer(blobClient -> leaseClient);

        // when
        var exc = catchThrowable(() -> leaseAcquirer.acquireFor(blobClient));

        // then
        assertThat(exc).isEqualTo(blobStorageException);
    }
}
