package uk.gov.hmcts.reform.blobrouter.services.storage;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.reform.blobrouter.data.envelopes.Envelope;
import uk.gov.hmcts.reform.blobrouter.services.EnvelopeService;

import java.util.UUID;

import static java.time.Instant.now;
import static java.util.Arrays.asList;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static uk.gov.hmcts.reform.blobrouter.data.envelopes.Status.REJECTED;

@ExtendWith(MockitoExtension.class)
@SuppressWarnings("checkstyle:LineLength")
class RejectedFilesHandlerTest {

    @Mock EnvelopeService envelopeService;
    @Mock BlobMover blobMover;

    final Envelope envelope1 = new Envelope(UUID.randomUUID(), "c1", "f1", now(), now(), null, REJECTED, false, false, 0);
    final Envelope envelope2 = new Envelope(UUID.randomUUID(), "c2", "f2", now(), now(), null, REJECTED, false, false, 0);

    RejectedFilesHandler mover;

    @BeforeEach
    void setUp() {
        mover = new RejectedFilesHandler(envelopeService, blobMover);
    }

    @Test
    void should_handle_rejected_files() {
        // given
        given(envelopeService.getReadyToDeleteRejections())
            .willReturn(asList(envelope1, envelope2));

        // when
        mover.handle();

        // then
        verify(blobMover).moveToRejectedContainer(envelope1.fileName, envelope1.container);
        verify(blobMover).moveToRejectedContainer(envelope2.fileName, envelope2.container);

        verify(envelopeService).markEnvelopeAsDeleted(envelope1);
        verify(envelopeService).markEnvelopeAsDeleted(envelope2);
    }

    @Test
    void should_continue_moving_files_after_failure() {
        // given
        given(envelopeService.getReadyToDeleteRejections())
            .willReturn(asList(envelope1, envelope2));

        doThrow(RuntimeException.class)
            .when(blobMover)
            .moveToRejectedContainer(envelope1.fileName, envelope1.container);

        // when
        mover.handle();

        // then second files should get processed anyway...
        verify(blobMover).moveToRejectedContainer(envelope2.fileName, envelope2.container);
        verify(envelopeService).markEnvelopeAsDeleted(envelope2);

        verify(envelopeService, never()).markEnvelopeAsDeleted(envelope1);
    }
}
