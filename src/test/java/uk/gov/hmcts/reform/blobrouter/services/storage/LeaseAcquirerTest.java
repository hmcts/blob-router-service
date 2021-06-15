package uk.gov.hmcts.reform.blobrouter.services.storage;

import com.azure.storage.blob.BlobClient;
import com.azure.storage.blob.models.BlobErrorCode;
import com.azure.storage.blob.models.BlobStorageException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.function.Consumer;

import static com.azure.storage.blob.models.BlobErrorCode.BLOB_NOT_FOUND;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

@ExtendWith(MockitoExtension.class)
@SuppressWarnings("unchecked")
class LeaseAcquirerTest {

    @Mock BlobClient blobClient;
    @Mock BlobStorageException blobStorageException;
    @Mock BlobMetaDataHandler blobMetaDataHandler;

    private LeaseAcquirer leaseAcquirer;

    @BeforeEach
    void setUp() {
        leaseAcquirer = new LeaseAcquirer(blobMetaDataHandler);
    }

    @Test
    void should_run_provided_action_when_metadata_lease_was_acquired() {
        // given
        var onSuccess = mock(Runnable.class);
        var onFailure = mock(Consumer.class);
        given(blobMetaDataHandler.isBlobReadyToUse(blobClient)).willReturn(true);

        // when
        leaseAcquirer.ifAcquiredOrElse(blobClient, onSuccess, onFailure, false);

        // then
        verify(onFailure, never()).accept(any(BlobErrorCode.class));
    }

    @Test
    void should_run_provided_action_when_metadata_lease_was_not_acquired() {
        // given
        doThrow(blobStorageException).when(blobMetaDataHandler).isBlobReadyToUse(blobClient);

        given(blobStorageException.getErrorCode()).willReturn(null);
        given(blobStorageException.getStatusCode()).willReturn(404);
        var onSuccess = mock(Runnable.class);
        var onFailure = mock(Consumer.class);

        // when
        leaseAcquirer.ifAcquiredOrElse(blobClient, onSuccess, onFailure, false);

        // then
        verify(onSuccess, never()).run();
        verify(onFailure).accept(BLOB_NOT_FOUND);
    }

    @Test
    void should_not_call_clear_metadata_when_failed_to_process_blob() {
        // given
        doThrow(blobStorageException).when(blobMetaDataHandler).isBlobReadyToUse(blobClient);

        // when
        leaseAcquirer.ifAcquiredOrElse(blobClient, mock(Runnable.class), mock(Consumer.class), true);

        // then
        verify(blobMetaDataHandler, never()).clearAllMetaData(any());
    }

    @Test
    void should_call_clear_metadata_when_successfully_processed_blob() {
        // given
        given(blobMetaDataHandler.isBlobReadyToUse(blobClient)).willReturn(true);

        // when
        leaseAcquirer.ifAcquiredOrElse(blobClient, mock(Runnable.class), mock(Consumer.class), true);

        // then
        verify(blobMetaDataHandler).clearAllMetaData(any());
    }

    @Test
    void should_run_onFailure_when_metadata_lease_can_not_acquired() {
        // given
        var onSuccess = mock(Runnable.class);
        var onFailure = mock(Consumer.class);
        given(blobMetaDataHandler.isBlobReadyToUse(blobClient)).willReturn(false);

        // when
        leaseAcquirer.ifAcquiredOrElse(blobClient, onSuccess, onFailure, false);

        // then
        verify(onSuccess, never()).run();
        verify(onFailure).accept(any(BlobErrorCode.class));
    }

    @Test
    void should_catch_exception_when_metadata_lease_clear_throw_exception() {
        // given
        var onSuccess = mock(Runnable.class);
        var onFailure = mock(Consumer.class);

        given(blobMetaDataHandler.isBlobReadyToUse(blobClient)).willReturn(true);
        willThrow(blobStorageException).given(blobMetaDataHandler).clearAllMetaData(blobClient);

        // when
        leaseAcquirer.ifAcquiredOrElse(blobClient, onSuccess, onFailure, true);

        // then
        verify(onSuccess).run();
        verify(onFailure, never()).accept(any());
        verifyNoMoreInteractions(blobMetaDataHandler);
    }
}
