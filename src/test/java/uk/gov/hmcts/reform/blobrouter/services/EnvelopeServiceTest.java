package uk.gov.hmcts.reform.blobrouter.services;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.reform.blobrouter.data.EnvelopeRepository;
import uk.gov.hmcts.reform.blobrouter.data.EventRecordRepository;
import uk.gov.hmcts.reform.blobrouter.data.model.Envelope;
import uk.gov.hmcts.reform.blobrouter.data.model.Event;
import uk.gov.hmcts.reform.blobrouter.data.model.NewEnvelope;
import uk.gov.hmcts.reform.blobrouter.data.model.NewEventRecord;
import uk.gov.hmcts.reform.blobrouter.data.model.Status;

import java.time.Instant;
import java.util.UUID;

import static java.time.Instant.now;
import static org.assertj.core.api.Assertions.assertThat;
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
    private EventRecordRepository eventRecordRepository;

    private EnvelopeService envelopeService;

    @BeforeEach
    void setUp() {
        envelopeService = new EnvelopeService(
            envelopeRepository,
            eventRecordRepository
        );
    }

    @Test
    void should_only_call_envelope_repository_to_get_envelope() {
        // when
        envelopeService.findEnvelope(BLOB_NAME, CONTAINER_NAME);

        // then
        verify(envelopeRepository).find(BLOB_NAME, CONTAINER_NAME);
        verifyNoInteractions(eventRecordRepository);
    }

    @Test
    void should_create_dispatched_envelope_as_expected() {
        // when
        envelopeService.createDispatchedEnvelope(CONTAINER_NAME, BLOB_NAME, BLOB_CREATED);

        // then
        var newEnvelopeCaptor = ArgumentCaptor.forClass(NewEnvelope.class);
        var newEventRecordCaptor = ArgumentCaptor.forClass(NewEventRecord.class);

        verify(envelopeRepository).insert(newEnvelopeCaptor.capture());
        verify(eventRecordRepository).insert(newEventRecordCaptor.capture());

        var envelope = newEnvelopeCaptor.getValue();
        var event = newEventRecordCaptor.getValue();

        assertThat(envelope.fileName).isEqualTo(BLOB_NAME);
        assertThat(envelope.container).isEqualTo(CONTAINER_NAME);
        assertThat(envelope.dispatchedAt).isNotNull();

        assertThat(event.fileName).isEqualTo(BLOB_NAME);
        assertThat(event.container).isEqualTo(CONTAINER_NAME);
        assertThat(event.event).isEqualTo(Event.DISPATCHED);
        assertThat(event.notes).isNull();
    }

    @Test
    void should_create_rejected_envelope_as_expected() {
        // when
        envelopeService.createRejectedEnvelope(CONTAINER_NAME, BLOB_NAME, BLOB_CREATED, REJECTION_REASON);

        // then
        var newEnvelopeCaptor = ArgumentCaptor.forClass(NewEnvelope.class);
        var newEventRecordCaptor = ArgumentCaptor.forClass(NewEventRecord.class);

        verify(envelopeRepository).insert(newEnvelopeCaptor.capture());
        verify(eventRecordRepository).insert(newEventRecordCaptor.capture());

        var envelope = newEnvelopeCaptor.getValue();
        var event = newEventRecordCaptor.getValue();

        assertThat(envelope.fileName).isEqualTo(BLOB_NAME);
        assertThat(envelope.container).isEqualTo(CONTAINER_NAME);
        assertThat(envelope.dispatchedAt).isNull();

        assertThat(event.fileName).isEqualTo(BLOB_NAME);
        assertThat(event.container).isEqualTo(CONTAINER_NAME);
        assertThat(event.event).isEqualTo(Event.REJECTED);
        assertThat(event.notes).isEqualTo(REJECTION_REASON);
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
        var newEventRecordCaptor = ArgumentCaptor.forClass(NewEventRecord.class);

        verify(envelopeRepository).insert(newEnvelopeCaptor.capture());
        verify(eventRecordRepository).insert(newEventRecordCaptor.capture());

        var envelope = newEnvelopeCaptor.getValue();
        var event = newEventRecordCaptor.getValue();

        assertThat(envelope.fileName).isEqualTo(BLOB_NAME);
        assertThat(envelope.container).isEqualTo(CONTAINER_NAME);
        assertThat(envelope.status).isEqualTo(Status.CREATED);
        assertThat(envelope.dispatchedAt).isNull();
        assertThat(envelope.fileCreatedAt).isEqualTo(BLOB_CREATED);

        assertThat(event.fileName).isEqualTo(BLOB_NAME);
        assertThat(event.container).isEqualTo(CONTAINER_NAME);
        assertThat(event.event).isEqualTo(Event.FILE_PROCESSING_STARTED);
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
        verifyNoInteractions(eventRecordRepository);
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
        var newEventRecordCaptor = ArgumentCaptor.forClass(NewEventRecord.class);
        verify(eventRecordRepository).insert(newEventRecordCaptor.capture());
        assertThat(newEventRecordCaptor.getValue().event).isEqualTo(Event.DELETED);
    }

    @Test
    void should_record_process_start_event() {
        // when
        envelopeService.saveEventFileProcessingStarted(CONTAINER_NAME, BLOB_NAME);

        // then
        var newEventRecordCaptor = ArgumentCaptor.forClass(NewEventRecord.class);
        verify(eventRecordRepository).insert(newEventRecordCaptor.capture());
        assertThat(newEventRecordCaptor.getValue().event).isEqualTo(Event.FILE_PROCESSING_STARTED);

        // and
        verifyNoInteractions(envelopeRepository);
    }

    @Test
    void should_record_deletion_from_rejected_container_event() {
        // when
        envelopeService.saveEventDeletedFromRejected(CONTAINER_NAME, BLOB_NAME);

        // then
        var newEventRecordCaptor = ArgumentCaptor.forClass(NewEventRecord.class);
        verify(eventRecordRepository).insert(newEventRecordCaptor.capture());
        assertThat(newEventRecordCaptor.getValue().event).isEqualTo(Event.DELETED_FROM_REJECTED);

        // and
        verifyNoInteractions(envelopeRepository);
    }

    @Test
    void should_record_duplicate_rejected_event() {
        // when
        envelopeService.saveEventDuplicateRejected(CONTAINER_NAME, BLOB_NAME);

        // then
        var newEventRecordCaptor = ArgumentCaptor.forClass(NewEventRecord.class);
        verify(eventRecordRepository).insert(newEventRecordCaptor.capture());
        assertThat(newEventRecordCaptor.getValue().event).isEqualTo(Event.DUPLICATE_REJECTED);

        // and
        verifyNoInteractions(envelopeRepository);
    }
}
