package uk.gov.hmcts.reform.blobrouter.services.storage;

import com.azure.storage.blob.BlobClient;
import com.azure.storage.blob.models.BlobStorageException;
import com.azure.storage.blob.specialized.BlobLeaseClient;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.function.Consumer;

import static java.util.UUID.randomUUID;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
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
    @Mock Consumer<String> leaseIdConsumer;

    @Test
    void should_run_provided_action_when_lease_was_acquired() {
        // given
        var onFailure = mock(Runnable.class);

        var leaseAcquirer = new LeaseAcquirer(blobClient -> leaseClient);

        var leaseId = randomUUID().toString();

        when(leaseClient.acquireLease(LEASE_DURATION_IN_SECONDS)).thenReturn(leaseId);

        // when
        leaseAcquirer.ifAcquiredOrElse(blobClient, leaseIdConsumer, onFailure);

        // then
        verify(leaseIdConsumer).accept(leaseId);
        verify(onFailure, never()).run();

        verify(leaseClient).releaseLease();
    }

    @Test
    void should_run_provided_action_when_lease_was_not_acquired() {
        // given
        var onFailure = mock(Runnable.class);

        doThrow(blobStorageException).when(leaseClient).acquireLease(LEASE_DURATION_IN_SECONDS);

        var leaseAcquirer = new LeaseAcquirer(blobClient -> leaseClient);

        // when
        leaseAcquirer.ifAcquiredOrElse(blobClient, leaseIdConsumer, onFailure);

        // then
        verify(leaseIdConsumer, never()).accept(null);
        verify(onFailure).run();

        verify(leaseClient, never()).releaseLease();
    }

    @Test
    void should_handle_error_when_releasing_lease() {
        // given
        var onFailure = mock(Runnable.class);
        var leaseId = randomUUID().toString();

        when(leaseClient.acquireLease(LEASE_DURATION_IN_SECONDS)).thenReturn(leaseId);
        doThrow(blobStorageException).when(leaseClient).releaseLease();

        var leaseAcquirer = new LeaseAcquirer(blobClient -> leaseClient);

        // when
        var exc = catchThrowable(() -> leaseAcquirer.ifAcquiredOrElse(blobClient, leaseIdConsumer, onFailure));

        // then
        assertThat(exc).isNull();
        verify(leaseIdConsumer).accept(leaseId);
        verify(onFailure, never()).run();

        verify(leaseClient).releaseLease();
    }
}
