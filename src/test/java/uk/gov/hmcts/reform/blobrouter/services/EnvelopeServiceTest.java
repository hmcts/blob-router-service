package uk.gov.hmcts.reform.blobrouter.services;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.util.function.Tuple2;
import uk.gov.hmcts.reform.blobrouter.data.envelopes.Envelope;
import uk.gov.hmcts.reform.blobrouter.data.envelopes.EnvelopeRepository;
import uk.gov.hmcts.reform.blobrouter.data.envelopes.NewEnvelope;
import uk.gov.hmcts.reform.blobrouter.data.envelopes.Status;
import uk.gov.hmcts.reform.blobrouter.data.events.EnvelopeEvent;
import uk.gov.hmcts.reform.blobrouter.data.events.EnvelopeEventRepository;
import uk.gov.hmcts.reform.blobrouter.data.events.ErrorCode;
import uk.gov.hmcts.reform.blobrouter.data.events.EventType;
import uk.gov.hmcts.reform.blobrouter.data.events.NewEnvelopeEvent;
import uk.gov.hmcts.reform.blobrouter.exceptions.EnvelopeNotFoundException;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Stream;

import static java.time.Instant.now;
import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
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
        envelopeService.findLastEnvelope(BLOB_NAME, CONTAINER_NAME);

        // then
        verify(envelopeRepository).findLast(BLOB_NAME, CONTAINER_NAME);
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
            false,
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
        var existingEnvelope = new Envelope(
            UUID.randomUUID(), "c", "f", null, null, null, Status.CREATED, false, false
        );
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
        var existingEnvelope = new Envelope(
            UUID.randomUUID(), "c", "f", null, null, null, Status.CREATED, false, false
        );
        given(envelopeRepository.find(existingEnvelope.id))
            .willReturn(Optional.of(existingEnvelope));

        // when
        envelopeService.markAsRejected(existingEnvelope.id, ErrorCode.ERR_METAFILE_INVALID, "some reason");

        // then
        verify(envelopeRepository).updateStatus(existingEnvelope.id, Status.REJECTED);
        verify(envelopeRepository).updatePendingNotification(existingEnvelope.id, true);

        var eventCaptor = ArgumentCaptor.forClass(NewEnvelopeEvent.class);
        verify(eventRepository).insert(eventCaptor.capture());

        assertThat(eventCaptor.getValue().envelopeId).isEqualTo(existingEnvelope.id);
        assertThat(eventCaptor.getValue().type).isEqualTo(EventType.REJECTED);
        assertThat(eventCaptor.getValue().errorCode).isEqualTo(ErrorCode.ERR_METAFILE_INVALID);
        assertThat(eventCaptor.getValue().notes).isEqualTo("some reason");
    }

    @Test
    void should_build_envelope_info() {
        // given
        var envelope1 = new Envelope(
            UUID.randomUUID(), "b", "a", now(), now(), now(), Status.DISPATCHED, true, false
        );
        var event1a = new EnvelopeEvent(1L, envelope1.id, EventType.FILE_PROCESSING_STARTED, null, null, now());
        var event1b = new EnvelopeEvent(2L, envelope1.id, EventType.DISPATCHED, null, null, now());

        var envelope2 = new Envelope(
            UUID.randomUUID(), "b", "a", now(), now(), now(), Status.REJECTED, true, false
        );
        var event2a = new EnvelopeEvent(3L, envelope2.id, EventType.FILE_PROCESSING_STARTED, null, null, now());
        var event2b = new EnvelopeEvent(4L, envelope2.id, EventType.REJECTED, ErrorCode.ERR_AV_FAILED, null, now());

        given(envelopeRepository.find("a", "b")).willReturn(asList(envelope1, envelope2));
        given(eventRepository.findForEnvelope(envelope1.id)).willReturn(asList(event1a, event1b));
        given(eventRepository.findForEnvelope(envelope2.id)).willReturn(asList(event2a, event2b));

        // when
        var result = envelopeService.getEnvelopeInfo("a", "b");

        // then
        assertThat(result).hasSize(2);

        assertThat(result.get(0).getT1()).isEqualToComparingFieldByField(envelope1);
        assertThat(result.get(0).getT2())
            .usingFieldByFieldElementComparator()
            .containsExactlyInAnyOrder(event1a, event1b);

        assertThat(result.get(1).getT1()).isEqualToComparingFieldByField(envelope2);
        assertThat(result.get(1).getT2())
            .usingFieldByFieldElementComparator()
            .containsExactlyInAnyOrder(event2a, event2b);
    }

    @Test
    void should_throw_exception_when_trying_to_mark_not_existing_envelope_as_dispatched() {
        // given
        var notExistingId = UUID.randomUUID();
        given(envelopeRepository.find(any()))
            .willReturn(Optional.empty());

        // when
        var exc = catchThrowable(() -> envelopeService.markAsRejected(notExistingId, null,"error"));

        // then
        assertThat(exc)
            .isInstanceOf(EnvelopeNotFoundException.class)
            .hasMessageContaining(notExistingId.toString());
    }

    @Test
    void should_mark_envelope_as_notification_sent() {
        // given
        var existingEnvelope = new Envelope(
            UUID.randomUUID(), "c", "f", null, null, null, Status.REJECTED, false, true
        );

        // when
        envelopeService.markPendingNotificationAsSent(existingEnvelope.id);

        // then
        verify(envelopeRepository).updatePendingNotification(existingEnvelope.id, false);

        var eventCaptor = ArgumentCaptor.forClass(NewEnvelopeEvent.class);
        verify(eventRepository).insert(eventCaptor.capture());

        assertThat(eventCaptor.getValue().envelopeId).isEqualTo(existingEnvelope.id);
        assertThat(eventCaptor.getValue().type).isEqualTo(EventType.NOTIFICATION_SENT);
    }

    @Test
    void should_call_envelope_repository_with_the_filename_container_and_requested_date_values() {
        // given
        var envelope1 = new Envelope(
            UUID.randomUUID(), "c1", "file1", now(), now(), now(), Status.DISPATCHED, true, false
        );
        var event1a = new EnvelopeEvent(1L, envelope1.id, EventType.FILE_PROCESSING_STARTED, null, null, now());
        var event1b = new EnvelopeEvent(2L, envelope1.id, EventType.DISPATCHED, null, null, now());

        var envelope2 = new Envelope(
            UUID.randomUUID(), "c1", "file2", now(), now(), now(), Status.REJECTED, true, false
        );
        var event2a = new EnvelopeEvent(3L, envelope2.id, EventType.FILE_PROCESSING_STARTED, null, null, now());

        LocalDate date = LocalDate.now();
        given(envelopeRepository.findEnvelopes("", "c1", date)).willReturn(asList(envelope1, envelope2));
        given(eventRepository.findForEnvelope(envelope1.id)).willReturn(asList(event1a, event1b));
        given(eventRepository.findForEnvelope(envelope2.id)).willReturn(singletonList(event2a));

        // when
        List<Tuple2<Envelope, List<EnvelopeEvent>>> envelopes = envelopeService.getEnvelopes(
            "", "c1", date
        );

        // then
        assertThat(envelopes).hasSize(2);

        assertThat(envelopes.get(0).getT1()).isEqualToComparingFieldByField(envelope1);
        assertThat(envelopes.get(0).getT2())
            .usingFieldByFieldElementComparator()
            .containsExactlyInAnyOrder(event1a, event1b);

        assertThat(envelopes.get(1).getT1()).isEqualToComparingFieldByField(envelope2);
        assertThat(envelopes.get(1).getT2())
            .usingFieldByFieldElementComparator()
            .containsOnly(event2a);
    }
}
