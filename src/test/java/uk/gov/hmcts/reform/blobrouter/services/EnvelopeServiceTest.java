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
import uk.gov.hmcts.reform.blobrouter.exceptions.InvalidRequestParametersException;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Stream;

import static java.time.Instant.now;
import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
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
        var id = envelopeService.createNewEnvelope(CONTAINER_NAME, BLOB_NAME, BLOB_CREATED, 1024L);

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
            false,
            null
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
            var notes = UUID.randomUUID().toString();

            // when
            envelopeService.saveEvent(envelopeId, eventType, notes);

            // then
            var eventCaptor = ArgumentCaptor.forClass(NewEnvelopeEvent.class);
            verify(eventRepository).insert(eventCaptor.capture());

            assertThat(eventCaptor.getValue().envelopeId).isEqualTo(envelopeId);
            assertThat(eventCaptor.getValue().type).isEqualTo(eventType);
            assertThat(eventCaptor.getValue().notes).isEqualTo(notes);

            reset(eventRepository);
        });
    }

    @Test
    void should_record_event_with_null_notes_when_no_argument_is_passed() {
        // given
        var envelopeId = UUID.randomUUID();
        var eventType = EventType.ERROR;

        // when
        envelopeService.saveEvent(envelopeId, eventType);

        // then
        var eventCaptor = ArgumentCaptor.forClass(NewEnvelopeEvent.class);
        verify(eventRepository).insert(eventCaptor.capture());

        assertThat(eventCaptor.getValue().envelopeId).isEqualTo(envelopeId);
        assertThat(eventCaptor.getValue().type).isEqualTo(eventType);
        assertThat(eventCaptor.getValue().notes).isNull();
    }

    @Test
    void should_mark_envelope_as_dispatched() {
        // given
        var existingEnvelope = new Envelope(
            UUID.randomUUID(), "c", "f", null, null, null, Status.CREATED, false, false, null
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
            UUID.randomUUID(), "c", "f", null, null, null, Status.CREATED, false, false, null
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
    void should_throw_exception_when_trying_to_mark_not_existing_envelope_as_dispatched() {
        // given
        var notExistingId = UUID.randomUUID();
        given(envelopeRepository.find(any()))
            .willReturn(Optional.empty());

        // when
        var exc = catchThrowable(() -> envelopeService.markAsRejected(notExistingId, null, "error"));

        // then
        assertThat(exc)
            .isInstanceOf(EnvelopeNotFoundException.class)
            .hasMessageContaining(notExistingId.toString());
    }

    @Test
    void should_mark_envelope_as_notification_sent() {
        // given
        var existingEnvelope = new Envelope(
            UUID.randomUUID(), "c", "f", null, null, null, Status.REJECTED, false, true, null
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
            UUID.randomUUID(), "c1", "file1", now(), now(), now(), Status.DISPATCHED, true, false, null
        );
        var event1a = new EnvelopeEvent(1L, envelope1.id, EventType.FILE_PROCESSING_STARTED, null, null, now());
        var event1b = new EnvelopeEvent(2L, envelope1.id, EventType.DISPATCHED, null, null, now().plusMillis(10));

        var envelope2 = new Envelope(
            UUID.randomUUID(), "c1", "file2", now().plusMillis(10), now(), now(), Status.REJECTED, true, false, null
        );
        var event2a = new EnvelopeEvent(3L, envelope2.id, EventType.FILE_PROCESSING_STARTED, null, null, now());

        var envelope3 = new Envelope(
            UUID.randomUUID(), "c1", "file3", now().plusMillis(10), now(), now(), Status.REJECTED, true, false, null
        );

        LocalDate date = LocalDate.now();
        given(envelopeRepository.findEnvelopes("", "c1", date)).willReturn(asList(envelope3, envelope2, envelope1));
        given(eventRepository.findForEnvelopes(asList(envelope3.id, envelope2.id, envelope1.id))).willReturn(
            asList(event2a, event1b, event1a) // should be ordered by event id
        );

        // when
        List<Tuple2<Envelope, List<EnvelopeEvent>>> envelopes = envelopeService.getEnvelopes("", "c1", date);

        // then
        assertThat(envelopes).hasSize(3);

        assertThat(envelopes.get(0).getT1()).isEqualToComparingFieldByField(envelope3);
        assertThat(envelopes.get(0).getT2()).isEmpty();

        assertThat(envelopes.get(1).getT1()).isEqualToComparingFieldByField(envelope2);
        assertThat(envelopes.get(1).getT2())
            .usingFieldByFieldElementComparator()
            .containsOnly(event2a);

        assertThat(envelopes.get(2).getT1()).isEqualToComparingFieldByField(envelope1);
        assertThat(envelopes.get(2).getT2())
            .usingFieldByFieldElementComparator()
            .containsExactly(event1b, event1a);
    }

    @Test
    void should_not_call_envelope_events_repository_when_no_envelopes_exists_for_the_given_filename() {
        // given
        given(envelopeRepository.findEnvelopes("f1.zip", null, null)).willReturn(emptyList());

        // when
        List<Tuple2<Envelope, List<EnvelopeEvent>>> envelopes = envelopeService.getEnvelopes("f1.zip", null, null);

        // then
        verify(envelopeRepository).findEnvelopes("f1.zip", null, null);
        assertThat(envelopes).isEmpty();
        verifyNoInteractions(eventRepository);
    }

    @Test
    void should_return_empty_events_list_when_no_events_created_for_an_envelope() {
        // given
        given(envelopeRepository.findEnvelopes("f1.zip", null, null)).willReturn(emptyList());

        // when
        List<Tuple2<Envelope, List<EnvelopeEvent>>> envelopes = envelopeService.getEnvelopes("f1.zip", null, null);

        // then
        verify(envelopeRepository).findEnvelopes("f1.zip", null, null);
        assertThat(envelopes).isEmpty();
        verifyNoInteractions(eventRepository);
    }

    @Test
    void should_throw_if_no_file_name_container_and_date_provided() {
        // given
        // when
        // then
        assertThatThrownBy(() -> envelopeService.getEnvelopes(null, null, null))
            .isInstanceOf(InvalidRequestParametersException.class)
            .hasMessageContaining("'file_name' or 'date' must not be null or empty");
    }

    @Test
    void should_throw_if_no_file_name_and_date_provided() {
        // given
        // when
        // then
        assertThatThrownBy(() -> envelopeService.getEnvelopes(null, "c1", null))
            .isInstanceOf(InvalidRequestParametersException.class)
            .hasMessageContaining("'file_name' or 'date' must not be null or empty");
    }

    @Test
    void should_return_emptyList_when_no_envelopes_exists_for_the_given_date() {
        // given
        LocalDate date = LocalDate.now();
        given(envelopeRepository.findEnvelopes(null, null, date)).willReturn(emptyList());

        // when
        List<Envelope> envelopes = envelopeService.getEnvelopes(date);

        // then
        verify(envelopeRepository).findEnvelopes(null, null, date);
        assertThat(envelopes).isEmpty();
        verifyNoInteractions(eventRepository);
    }

    @Test
    void should_return_envelope_list_when_envelopes_exists_for_the_given_date() {
        // given
        LocalDate date = LocalDate.now();
        var envelope1 = new Envelope(
            UUID.randomUUID(), "c1", "file1", now(), now(), now(), Status.DISPATCHED, true, false, null
        );

        var envelope2 = new Envelope(
            UUID.randomUUID(), "c1", "file2", now().plusMillis(10), now(), now(), Status.REJECTED, true, false, null
        );
        var list = List.of(envelope1, envelope2);
        given(envelopeRepository.findEnvelopes(null, null, date)).willReturn(list);

        // when
        List<Envelope> envelopes = envelopeService.getEnvelopes(date);

        // then
        verify(envelopeRepository).findEnvelopes(null, null, date);
        assertThat(envelopes)
            .containsAll(list)
            .isNotSameAs(list);
        verifyNoInteractions(eventRepository);
    }

    @Test
    void should_find_envelope_not_in_created_status() {
        // given
        String fileName = "file123.zip";
        String containerName = "X";
        var envelope =  mock(Envelope.class);
        given(envelopeRepository.findEnvelopeNotInCreatedStatus(fileName, containerName))
            .willReturn(Optional.of(envelope));

        // when
        Optional<Envelope> envelopeOpt = envelopeService.findEnvelopeNotInCreatedStatus(fileName,containerName);

        // then
        assertThat(envelopeOpt).hasValue(envelope);
        verify(envelopeRepository).findEnvelopeNotInCreatedStatus(fileName, containerName);
        verifyNoInteractions(eventRepository);
    }

    @Test
    void should_not_find_envelope_not_in_created_status() {
        // given
        String fileName = "file123.zip";
        String containerName = "X";
        given(envelopeRepository.findEnvelopeNotInCreatedStatus(fileName, containerName))
            .willReturn(Optional.empty());

        // when
        Optional<Envelope> envelopeOpt = envelopeService.findEnvelopeNotInCreatedStatus(fileName,containerName);

        // then
        assertThat(envelopeOpt).isEmpty();
        verify(envelopeRepository).findEnvelopeNotInCreatedStatus(fileName, containerName);
        verifyNoInteractions(eventRepository);
    }

    @Test
    void should_return_emptyList_when_no_envelopes_exists_for_dcn_for_given_dates() {
        // given
        LocalDate fromDate = LocalDate.now().minusDays(3);
        LocalDate toDate = LocalDate.now();
        String dcnPrefix = "23131312";
        given(envelopeRepository.findEnvelopesByDcnPrefix(dcnPrefix, fromDate, toDate)).willReturn(emptyList());

        // when
        List<Envelope> envelopes = envelopeService.getEnvelopesByDcnPrefix(dcnPrefix, fromDate, toDate);

        // then
        verify(envelopeRepository).findEnvelopesByDcnPrefix(dcnPrefix, fromDate, toDate);
        assertThat(envelopes).isEmpty();
        verifyNoInteractions(eventRepository);
    }

    @Test
    void should_return_envelopeList_when_envelope_exists_for_dcn_for_given_dates() {
        // given
        String dcnPrefix = "23131312";
        var envelope1 = new Envelope(
            UUID.randomUUID(), "c1", "file1", now(), now(), now(), Status.DISPATCHED, true, false, null
        );
        var envelope2 = new Envelope(
            UUID.randomUUID(), "c2", "file2", now(), now(), now(), Status.REJECTED, false, false, null
        );
        LocalDate fromDate = LocalDate.now().minusDays(3);
        LocalDate toDate = LocalDate.now();
        given(envelopeRepository.findEnvelopesByDcnPrefix(dcnPrefix, fromDate, toDate))
            .willReturn(asList(envelope2, envelope1));

        // when
        List<Envelope> envelopes = envelopeService.getEnvelopesByDcnPrefix(dcnPrefix, fromDate, toDate);

        // then
        verify(envelopeRepository).findEnvelopesByDcnPrefix(dcnPrefix, fromDate, toDate);
        assertThat(envelopes).isEqualTo(asList(envelope2, envelope1));
        verifyNoInteractions(eventRepository);
    }

}
