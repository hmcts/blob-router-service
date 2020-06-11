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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
@SuppressWarnings("unchecked")
class LeaseAcquirerTest {

    @Mock BlobClient blobClient;
    @Mock BlobLeaseClient leaseClient;
    @Mock BlobStorageException blobStorageException;

    private LeaseAcquirer leaseAcquirer;

    @BeforeEach
    void setUp() {
        leaseAcquirer = new LeaseAcquirer(blobClient -> leaseClient);
    }

    @Test
    void should_run_provided_action_when_lease_was_acquired() {
        // given
        var onSuccess = mock(Consumer.class);
        var onFailure = mock(Consumer.class);

        // when
        leaseAcquirer.ifAcquiredOrElse(blobClient, onSuccess, onFailure);

        // then
        verify(onSuccess).accept(null);
        verify(onFailure, never()).accept(any(BlobErrorCode.class));
        verify(leaseClient).releaseLease();
    }

    @Test
    void should_run_provided_action_when_lease_was_not_acquired() {
        // given
        var onSuccess = mock(Consumer.class);
        var onFailure = mock(Consumer.class);

        doThrow(blobStorageException).when(leaseClient).acquireLease(anyInt());

        // when
        leaseAcquirer.ifAcquiredOrElse(blobClient, onSuccess, onFailure);

        // then
        verify(onSuccess, never()).accept(anyString());
        verify(onFailure).accept(null);
        verify(leaseClient, never()).releaseLease();
    }

    @Test
    void should_handle_error_when_releasing_lease() {
        // given
        var onSuccess = mock(Consumer.class);
        var onFailure = mock(Consumer.class);

        doThrow(blobStorageException).when(leaseClient).releaseLease();

        // when
        var exc = catchThrowable(() -> leaseAcquirer.ifAcquiredOrElse(blobClient, onSuccess, onFailure));

        // then
        assertThat(exc).isNull();
        verify(onSuccess).accept(null);
        verify(onFailure, never()).accept(any(BlobErrorCode.class));
        verify(leaseClient).releaseLease();
    }
}
