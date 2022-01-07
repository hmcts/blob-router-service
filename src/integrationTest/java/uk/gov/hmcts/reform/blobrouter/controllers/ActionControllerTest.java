package uk.gov.hmcts.reform.blobrouter.controllers;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;
import uk.gov.hmcts.reform.blobrouter.clients.bulkscanprocessor.BulkScanProcessorClient;
import uk.gov.hmcts.reform.blobrouter.clients.response.ZipFileResponse;
import uk.gov.hmcts.reform.blobrouter.data.envelopes.Envelope;
import uk.gov.hmcts.reform.blobrouter.data.envelopes.EnvelopeRepository;
import uk.gov.hmcts.reform.blobrouter.data.envelopes.Status;
import uk.gov.hmcts.reform.blobrouter.data.events.EnvelopeEvent;
import uk.gov.hmcts.reform.blobrouter.data.events.EnvelopeEventRepository;
import uk.gov.hmcts.reform.blobrouter.data.events.EventType;
import uk.gov.hmcts.reform.blobrouter.data.events.NewEnvelopeEvent;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static java.time.Instant.now;
import static java.time.temporal.ChronoUnit.HOURS;
import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static uk.gov.hmcts.reform.blobrouter.data.events.ErrorCode.ERR_STALE_ENVELOPE;
import static uk.gov.hmcts.reform.blobrouter.data.events.EventType.DELETED;
import static uk.gov.hmcts.reform.blobrouter.data.events.EventType.FILE_PROCESSING_STARTED;
import static uk.gov.hmcts.reform.blobrouter.data.events.EventType.MANUALLY_MARKED_AS_DISPATCHED;
import static uk.gov.hmcts.reform.blobrouter.data.events.EventType.MANUALLY_MARKED_AS_REJECTED;

@AutoConfigureMockMvc
@SpringBootTest
class ActionControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ActionController actionController;

    @MockBean
    private EnvelopeRepository envelopeRepository;

    @MockBean
    private EnvelopeEventRepository envelopeEventRepository;

    @MockBean
    private BulkScanProcessorClient bulkScanProcessorClient;

    @Test
    void should_respond_ok_if_envelope_has_created_status_and_stale_events_and_known_to_processor()
        throws Exception {

        UUID envelopeId = UUID.randomUUID();

        Envelope envelope = envelope(envelopeId, Status.CREATED);
        Optional<Envelope> envelopeOpt = Optional.of(envelope);
        given(envelopeRepository.find(envelopeId)).willReturn(envelopeOpt);

        ZipFileResponse response = new ZipFileResponse(
            envelope.fileName,
            singletonList(new Object()),
            emptyList()
        );
        given(bulkScanProcessorClient.getZipFile(envelope.fileName)).willReturn(response);

        Instant threeHoursAgo = Instant.now().minus(3, HOURS);
        Instant fourHoursAgo = Instant.now().minus(4, HOURS);
        Instant fiveHoursAgo = Instant.now().minus(5, HOURS);
        EnvelopeEvent event1 = envelopeEvent(envelopeId, fiveHoursAgo, 1L, FILE_PROCESSING_STARTED);
        EnvelopeEvent event2 = envelopeEvent(envelopeId, fourHoursAgo, 2L, EventType.DISPATCHED);
        EnvelopeEvent event3 = envelopeEvent(envelopeId, threeHoursAgo, 3L, DELETED);
        given(envelopeEventRepository.findForEnvelope(envelopeId))
            .willReturn(asList(event1, event2, event3));

        mockMvc
            .perform(
                put("/actions/complete/" + envelopeId)
            )
            .andExpect(status().isOk());

        var envelopeEventCaptor = ArgumentCaptor.forClass(NewEnvelopeEvent.class);
        verify(envelopeEventRepository, times(2)).insert(envelopeEventCaptor.capture());
        List<NewEnvelopeEvent> capturedEnvelopeEvents = envelopeEventCaptor.getAllValues();
        assertThat(capturedEnvelopeEvents.get(0).envelopeId).isEqualTo(envelopeId);
        assertThat(capturedEnvelopeEvents.get(0).type).isEqualTo(MANUALLY_MARKED_AS_DISPATCHED);
        assertThat(capturedEnvelopeEvents.get(0).errorCode).isEqualTo(ERR_STALE_ENVELOPE);
        assertThat(capturedEnvelopeEvents.get(0).notes)
            .isEqualTo("Manually marked as dispatched due to stale state");
        assertThat(capturedEnvelopeEvents.get(1).envelopeId).isEqualTo(envelopeId);
        assertThat(capturedEnvelopeEvents.get(1).type).isEqualTo(DELETED);
        assertThat(capturedEnvelopeEvents.get(1).errorCode).isEqualTo(ERR_STALE_ENVELOPE);
        assertThat(capturedEnvelopeEvents.get(1).notes)
            .isEqualTo("Manually marked as deleted due to stale state");
    }

    @Test
    void should_respond_ok_if_envelope_has_created_status_and_stale_events_and_not_known_to_processor()
        throws Exception {

        UUID envelopeId = UUID.randomUUID();

        Envelope envelope = envelope(envelopeId, Status.CREATED);
        Optional<Envelope> envelopeOpt = Optional.of(envelope);
        given(envelopeRepository.find(envelopeId)).willReturn(envelopeOpt);

        ZipFileResponse response = new ZipFileResponse(
            envelope.fileName,
            emptyList(),
            emptyList()
        );
        given(bulkScanProcessorClient.getZipFile(envelope.fileName)).willReturn(response);

        Instant threeHoursAgo = Instant.now().minus(3, HOURS);
        Instant fourHoursAgo = Instant.now().minus(4, HOURS);
        Instant fiveHoursAgo = Instant.now().minus(5, HOURS);
        EnvelopeEvent event1 = envelopeEvent(envelopeId, fiveHoursAgo, 1L, FILE_PROCESSING_STARTED);
        EnvelopeEvent event2 = envelopeEvent(envelopeId, fourHoursAgo, 2L, EventType.DISPATCHED);
        EnvelopeEvent event3 = envelopeEvent(envelopeId, threeHoursAgo, 3L, DELETED);
        given(envelopeEventRepository.findForEnvelope(envelopeId))
            .willReturn(asList(event1, event2, event3));

        mockMvc
            .perform(
                put("/actions/complete/" + envelopeId)
            )
            .andExpect(status().isOk());

        var envelopeEventCaptor = ArgumentCaptor.forClass(NewEnvelopeEvent.class);
        verify(envelopeEventRepository, times(2)).insert(envelopeEventCaptor.capture());
        List<NewEnvelopeEvent> capturedEnvelopeEvents = envelopeEventCaptor.getAllValues();
        assertThat(capturedEnvelopeEvents.get(0).envelopeId).isEqualTo(envelopeId);
        assertThat(capturedEnvelopeEvents.get(0).type).isEqualTo(MANUALLY_MARKED_AS_REJECTED);
        assertThat(capturedEnvelopeEvents.get(0).errorCode).isEqualTo(ERR_STALE_ENVELOPE);
        assertThat(capturedEnvelopeEvents.get(0).notes)
            .isEqualTo("Manually marked as rejected due to stale state");
        assertThat(capturedEnvelopeEvents.get(1).envelopeId).isEqualTo(envelopeId);
        assertThat(capturedEnvelopeEvents.get(1).type).isEqualTo(DELETED);
        assertThat(capturedEnvelopeEvents.get(1).errorCode).isEqualTo(ERR_STALE_ENVELOPE);
        assertThat(capturedEnvelopeEvents.get(1).notes)
            .isEqualTo("Manually marked as deleted due to stale state");
    }

    @Test
    void should_respond_conflict_if_envelope_has_created_status_and_non_stale_events()
        throws Exception {

        UUID envelopeId = UUID.randomUUID();

        Envelope envelope = envelope(envelopeId, Status.CREATED);
        Optional<Envelope> envelopeOpt = Optional.of(envelope);
        given(envelopeRepository.find(envelopeId)).willReturn(envelopeOpt);

        ZipFileResponse response = new ZipFileResponse(
            envelope.fileName,
            emptyList(),
            emptyList()
        );
        given(bulkScanProcessorClient.getZipFile(envelope.fileName)).willReturn(response);

        Instant oneHourAgo = Instant.now().minus(1, HOURS);
        Instant fourHoursAgo = Instant.now().minus(4, HOURS);
        Instant fiveHoursAgo = Instant.now().minus(5, HOURS);
        EnvelopeEvent event1 = envelopeEvent(envelopeId, fiveHoursAgo, 1L, FILE_PROCESSING_STARTED);
        EnvelopeEvent event2 = envelopeEvent(envelopeId, fourHoursAgo, 2L, EventType.DISPATCHED);
        EnvelopeEvent event3 = envelopeEvent(envelopeId, oneHourAgo, 3L, DELETED);
        given(envelopeEventRepository.findForEnvelope(envelopeId))
            .willReturn(asList(event1, event2, event3));

        mockMvc
            .perform(
                put("/actions/complete/" + envelopeId)
            )
            .andExpect(status().isConflict());

        verify(envelopeEventRepository).findForEnvelope(envelopeId);
        verifyNoMoreInteractions(envelopeEventRepository);
        verifyNoInteractions(bulkScanProcessorClient);
    }

    @Test
    void should_respond_conflict_if_envelope_has_dispatched_status() throws Exception {

        UUID envelopeId = UUID.randomUUID();

        Envelope envelope = envelope(envelopeId, Status.DISPATCHED);
        Optional<Envelope> envelopeOpt = Optional.of(envelope);
        given(envelopeRepository.find(envelopeId)).willReturn(envelopeOpt);

        mockMvc
            .perform(
                put("/actions/complete/" + envelopeId)
            )
            .andExpect(status().isConflict());

        verifyNoInteractions(envelopeEventRepository);
        verifyNoInteractions(bulkScanProcessorClient);
    }

    @Test
    void should_respond_conflict_if_envelope_has_rejected_status() throws Exception {

        UUID envelopeId = UUID.randomUUID();

        Envelope envelope = envelope(envelopeId, Status.REJECTED);
        Optional<Envelope> envelopeOpt = Optional.of(envelope);
        given(envelopeRepository.find(envelopeId)).willReturn(envelopeOpt);

        mockMvc
            .perform(
                put("/actions/complete/" + envelopeId)
            )
            .andExpect(status().isConflict());

        verifyNoInteractions(envelopeEventRepository);
        verifyNoInteractions(bulkScanProcessorClient);
    }

    @Test
    void should_respond_bad_request_if_uuid_corrupted() throws Exception {

        mockMvc
            .perform(
                put("/actions/complete/" + "corrupted")
            )
            .andExpect(status().isBadRequest());

        verifyNoInteractions(envelopeRepository);
        verifyNoInteractions(envelopeEventRepository);
    }

    @Test
    void should_respond_not_found_if_envelope_does_not_exist() throws Exception {

        UUID envelopeId = UUID.randomUUID();

        Optional<Envelope> envelopeOpt = Optional.empty();
        given(envelopeRepository.find(envelopeId)).willReturn(envelopeOpt);

        mockMvc
            .perform(
                put("/actions/complete/" + envelopeId)
            )
            .andExpect(status().isNotFound());

        verifyNoInteractions(envelopeEventRepository);
    }

    private Envelope envelope(UUID id, Status status) {
        return new Envelope(
            id,
            "some_container",
            "hello.zip",
            now(),
            null,
            null,
            status,
            false,
            false,
            null
        );
    }

    private EnvelopeEvent envelopeEvent(
        UUID envelopeId,
        Instant twoHoursAgo,
        long eventId,
        EventType eventType
    ) {
        return new EnvelopeEvent(
            eventId,
            envelopeId,
            eventType,
            null,
            null,
            twoHoursAgo
        );
    }
}
