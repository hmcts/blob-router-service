package uk.gov.hmcts.reform.blobrouter.services;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.reform.blobrouter.data.envelopes.Envelope;
import uk.gov.hmcts.reform.blobrouter.data.envelopes.EnvelopeRepository;
import uk.gov.hmcts.reform.blobrouter.data.envelopes.NewEnvelope;
import uk.gov.hmcts.reform.blobrouter.data.envelopes.Status;
import uk.gov.hmcts.reform.blobrouter.data.events.EnvelopeEventRepository;
import uk.gov.hmcts.reform.blobrouter.data.events.EventType;
import uk.gov.hmcts.reform.blobrouter.data.events.NewEnvelopeEvent;
import uk.gov.hmcts.reform.blobrouter.exceptions.EnvelopeNotFoundException;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Stream;

import static java.time.Instant.now;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

@ExtendWith(MockitoExtension.class)
class EnvelopeServiceTest {

    private static final String BLOB_NAME = "blob";
    private static final String CONTAINER_NAME = "container";
    private static final String REJECTION_REASON = "some rejection reason";
    private static final Instant BLOB_CREATED = now();

    @Mock
    private EnvelopeRepository envelopeRepository;

    @Mock
    private EnvelopeEventRepository eventRepository;

    private EnvelopeService envelopeService;

    @BeforeEach
    void setUp() {
        envelopeService = new EnvelopeService(
            envelopeRepository,
            eventRepository
        );
    }

    @Test
    void should_only_call_envelope_repository_to_get_envelope() {
        // when
        envelopeService.findEnvelope(BLOB_NAME, CONTAINER_NAME);

        // then
        verify(envelopeRepository).find(BLOB_NAME, CONTAINER_NAME);
        verifyNoInteractions(eventRepository);
    }

    @Test
    void should_create_new_envelope() {
        // given
        var idFromDb = UUID.randomUUID();
        given(envelopeRepository.insert(any())).willReturn(idFromDb);

        // when
        var id = envelopeService.createNewEnvelope(CONTAINER_NAME, BLOB_NAME, BLOB_CREATED);

        // then
        assertThat(id).isEqualTo(idFromDb);

        var newEnvelopeCaptor = ArgumentCaptor.forClass(NewEnvelope.class);
        var newEventCaptor = ArgumentCaptor.forClass(NewEnvelopeEvent.class);

        verify(envelopeRepository).insert(newEnvelopeCaptor.capture());
        verify(eventRepository).insert(newEventCaptor.capture());

        var envelope = newEnvelopeCaptor.getValue();
        var event = newEventCaptor.getValue();

        assertThat(envelope.fileName).isEqualTo(BLOB_NAME);
        assertThat(envelope.container).isEqualTo(CONTAINER_NAME);
        assertThat(envelope.status).isEqualTo(Status.CREATED);
        assertThat(envelope.dispatchedAt).isNull();
        assertThat(envelope.fileCreatedAt).isEqualTo(BLOB_CREATED);

        assertThat(event.type).isEqualTo(EventType.FILE_PROCESSING_STARTED);
        assertThat(event.notes).isNull();
    }

    @Test
    void should_only_call_envelope_repository_to_get_ready_to_delete_blobs() {
        // when
        envelopeService.getReadyToDeleteDispatches(CONTAINER_NAME);
        envelopeService.getReadyToDeleteRejections();

        // then
        verify(envelopeRepository).find(Status.DISPATCHED, CONTAINER_NAME, false);
        verify(envelopeRepository).find(Status.REJECTED, false);
        verifyNoInteractions(eventRepository);
    }

    @Test
    void should_mark_envelope_as_deleted_and_record_event() {
        // given
        var envelope = new Envelope(
            UUID.randomUUID(),
            CONTAINER_NAME,
            BLOB_NAME,
            now(),
            BLOB_CREATED,
            now(),
            Status.DISPATCHED,
            false
        );

        // when
        envelopeService.markEnvelopeAsDeleted(envelope);

        // then
        verify(envelopeRepository).markAsDeleted(envelope.id);

        // and (will be enabled once events recorded)
        var newEventCaptor = ArgumentCaptor.forClass(NewEnvelopeEvent.class);
        verify(eventRepository).insert(newEventCaptor.capture());
        assertThat(newEventCaptor.getValue().type).isEqualTo(EventType.DELETED);
    }

    @Test
    void should_record_event() {
        Stream.of(EventType.values()).forEach(eventType -> {
            // given
            var envelopeId = UUID.randomUUID();

            // when
            envelopeService.saveEvent(envelopeId, eventType);

            // then
            var eventCaptor = ArgumentCaptor.forClass(NewEnvelopeEvent.class);
            verify(eventRepository).insert(eventCaptor.capture());

            assertThat(eventCaptor.getValue().envelopeId).isEqualTo(envelopeId);
            assertThat(eventCaptor.getValue().type).isEqualTo(eventType);

            reset(eventRepository);
        });
    }

    @Test
    void should_mark_envelope_as_dispatched() {
        // given
        var existingEnvelope = new Envelope(UUID.randomUUID(), "c", "f", null, null, null, Status.CREATED, false);
        given(envelopeRepository.find(existingEnvelope.id))
            .willReturn(Optional.of(existingEnvelope));

        // when
        envelopeService.markAsDispatched(existingEnvelope.id);

        // then
        verify(envelopeRepository).updateStatus(existingEnvelope.id, Status.DISPATCHED);
        verify(envelopeRepository).updateDispatchDateTime(eq(existingEnvelope.id), any());

        var eventCaptor = ArgumentCaptor.forClass(NewEnvelopeEvent.class);
        verify(eventRepository).insert(eventCaptor.capture());

        assertThat(eventCaptor.getValue().envelopeId).isEqualTo(existingEnvelope.id);
        assertThat(eventCaptor.getValue().type).isEqualTo(EventType.DISPATCHED);
    }

    @Test
    void should_throw_exceptiopn_when_trying_to_mark_not_existing_envelope_as_dispatched() {
        // given
        var notExistingId = UUID.randomUUID();
        given(envelopeRepository.find(any()))
            .willReturn(Optional.empty());

        // when
        var exc = catchThrowable(() -> envelopeService.markAsDispatched(notExistingId));

        // then
        assertThat(exc)
            .isInstanceOf(EnvelopeNotFoundException.class)
            .hasMessageContaining(notExistingId.toString());
    }

    @Test
    void should_mark_envelope_as_rejected() {
        // given
        var existingEnvelope = new Envelope(UUID.randomUUID(), "c", "f", null, null, null, Status.CREATED, false);
        given(envelopeRepository.find(existingEnvelope.id))
            .willReturn(Optional.of(existingEnvelope));

        // when
        envelopeService.markAsRejected(existingEnvelope.id);

        // then
        verify(envelopeRepository).updateStatus(existingEnvelope.id, Status.REJECTED);

        var eventCaptor = ArgumentCaptor.forClass(NewEnvelopeEvent.class);
        verify(eventRepository).insert(eventCaptor.capture());

        assertThat(eventCaptor.getValue().envelopeId).isEqualTo(existingEnvelope.id);
        assertThat(eventCaptor.getValue().type).isEqualTo(EventType.REJECTED);
    }

    @Test
    void should_throw_exception_when_trying_to_mark_not_existing_envelope_as_dispatched() {
        // given
        var notExistingId = UUID.randomUUID();
        given(envelopeRepository.find(any()))
            .willReturn(Optional.empty());

        // when
        var exc = catchThrowable(() -> envelopeService.markAsRejected(notExistingId));

        // then
        assertThat(exc)
            .isInstanceOf(EnvelopeNotFoundException.class)
            .hasMessageContaining(notExistingId.toString());
    }
}
