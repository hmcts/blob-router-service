package uk.gov.hmcts.reform.blobrouter.services;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.reform.blobrouter.data.EnvelopeRepository;
import uk.gov.hmcts.reform.blobrouter.data.EventRecordRepository;
import uk.gov.hmcts.reform.blobrouter.data.model.Event;
import uk.gov.hmcts.reform.blobrouter.data.model.NewEnvelope;
import uk.gov.hmcts.reform.blobrouter.data.model.NewEventRecord;
import uk.gov.hmcts.reform.blobrouter.data.model.Status;

import java.time.Instant;
import java.util.UUID;

import static java.time.Instant.now;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

@ExtendWith(MockitoExtension.class)
class EnvelopeServiceTest {

    private static final String BLOB_NAME = "blob";
    private static final String CONTAINER_NAME = "container";
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
    void should_create_new_envelope_and_record_event() {
        // when
        envelopeService.createDispatchedEnvelope(CONTAINER_NAME, BLOB_NAME, BLOB_CREATED);
        envelopeService.createRejectedEnvelope(CONTAINER_NAME, BLOB_NAME, BLOB_CREATED);

        // then
        var newEnvelopeCaptor = ArgumentCaptor.forClass(NewEnvelope.class);
        verify(envelopeRepository, times(2)).insert(newEnvelopeCaptor.capture());
        assertThat(newEnvelopeCaptor.getAllValues())
            .hasSize(2)
            .extracting(item -> item.status)
            .containsOnly(Status.DISPATCHED, Status.REJECTED);

        // and (will be enabled once events recorded)
        var newEventRecordCaptor = ArgumentCaptor.forClass(NewEventRecord.class);
        verify(eventRecordRepository, never()).insert(newEventRecordCaptor.capture());
        assertThat(newEventRecordCaptor.getAllValues())
            .isEmpty();
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
        var envelopeId = UUID.randomUUID();

        // when
        envelopeService.markEnvelopeAsDeleted(envelopeId);

        // then
        verify(envelopeRepository).markAsDeleted(envelopeId);

        // and (will be enabled once events recorded)
        var newEventRecordCaptor = ArgumentCaptor.forClass(NewEventRecord.class);
        verify(eventRecordRepository, never()).insert(newEventRecordCaptor.capture());
        assertThat(newEventRecordCaptor.getAllValues()).isEmpty();
    }

    @Test
    void should_record_process_start_event() {
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
