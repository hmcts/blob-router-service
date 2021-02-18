package uk.gov.hmcts.reform.blobrouter.services;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.reform.blobrouter.data.envelopes.Envelope;
import uk.gov.hmcts.reform.blobrouter.data.envelopes.EnvelopeRepository;
import uk.gov.hmcts.reform.blobrouter.data.envelopes.Status;
import uk.gov.hmcts.reform.blobrouter.data.events.EnvelopeEvent;
import uk.gov.hmcts.reform.blobrouter.data.events.EnvelopeEventRepository;
import uk.gov.hmcts.reform.blobrouter.data.events.EventType;
import uk.gov.hmcts.reform.blobrouter.data.events.NewEnvelopeEvent;
import uk.gov.hmcts.reform.blobrouter.exceptions.EnvelopeCompletedOrNotStaleException;
import uk.gov.hmcts.reform.blobrouter.exceptions.EnvelopeNotFoundException;

import java.time.Instant;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.UUID;

import static java.time.temporal.ChronoUnit.HOURS;
import static java.time.temporal.ChronoUnit.MINUTES;
import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static uk.gov.hmcts.reform.blobrouter.data.envelopes.Status.CREATED;
import static uk.gov.hmcts.reform.blobrouter.data.envelopes.Status.DISPATCHED;
import static uk.gov.hmcts.reform.blobrouter.data.envelopes.Status.REJECTED;
import static uk.gov.hmcts.reform.blobrouter.data.events.ErrorCode.ERR_STALE_ENVELOPE;
import static uk.gov.hmcts.reform.blobrouter.data.events.EventType.DELETED;
import static uk.gov.hmcts.reform.blobrouter.data.events.EventType.FILE_PROCESSING_STARTED;
import static uk.gov.hmcts.reform.blobrouter.data.events.EventType.MANUALLY_REJECTED;

@ExtendWith(MockitoExtension.class)
class EnvelopeActionServiceTest {
    private EnvelopeActionService envelopeActionService;

    @Mock
    private EnvelopeRepository envelopeRepository;

    @Mock
    private EnvelopeEventRepository envelopeEventRepository;

    @BeforeEach
    void setUp() {
        envelopeActionService = new EnvelopeActionService(
            envelopeRepository,
            envelopeEventRepository,
            1
        );
    }

    @Test
    void should_throw_exception_if_envelope_does_not_exist() {
        // given
        var uuid = UUID.randomUUID();

        given(envelopeRepository.find(uuid)).willReturn(Optional.empty());

        // when
        // then
        assertThatThrownBy(() ->
                               envelopeActionService.rejectStaleEnvelope(uuid)
        )
            .isInstanceOf(EnvelopeNotFoundException.class)
            .hasMessageContaining("Envelope with id " + uuid + " not found");
    }

    @Test
    void should_save_envelope_and_event_if_envelope_is_stale() {
        // given
        var uuid = UUID.randomUUID();
        var envelope = envelope(uuid, CREATED);

        Instant twoHoursAgo = Instant.now().minus(2, HOURS);
        Instant threeHoursAgo = Instant.now().minus(3, HOURS);
        EnvelopeEvent event1 = event(uuid, threeHoursAgo, 2L, FILE_PROCESSING_STARTED);
        EnvelopeEvent event2 = event(uuid, twoHoursAgo, 1L, DELETED);

        given(envelopeRepository.find(uuid)).willReturn(Optional.of(envelope));
        given(envelopeEventRepository.findForEnvelope(uuid))
            .willReturn(asList(event1, event2));

        // when
        envelopeActionService.rejectStaleEnvelope(uuid);

        // then
        var envelopeEventCaptor = ArgumentCaptor.forClass(NewEnvelopeEvent.class);
        verify(envelopeEventRepository).insert(envelopeEventCaptor.capture());
        assertThat(envelopeEventCaptor.getValue().envelopeId).isEqualTo(uuid);
        assertThat(envelopeEventCaptor.getValue().type).isEqualTo(MANUALLY_REJECTED);
        assertThat(envelopeEventCaptor.getValue().errorCode).isEqualTo(ERR_STALE_ENVELOPE);
        assertThat(envelopeEventCaptor.getValue().notes)
            .isEqualTo("Manually rejected due to stale state");

        var idCaptor = ArgumentCaptor.forClass(UUID.class);
        var statusCaptor = ArgumentCaptor.forClass(Status.class);
        verify(envelopeRepository).updateStatus(idCaptor.capture(), statusCaptor.capture());
        assertThat(idCaptor.getValue()).isEqualTo(uuid);
        assertThat(statusCaptor.getValue()).isEqualTo(REJECTED);
    }

    @Test
    void should_throw_exception_if_envelope_is_not_stale() {
        // given
        var uuid = UUID.randomUUID();
        var envelope = envelope(uuid, CREATED);

        Instant halfHourAgo = Instant.now().minus(30, MINUTES);
        Instant threeHoursAgo = Instant.now().minus(3, HOURS);
        EnvelopeEvent event1 = event(uuid, threeHoursAgo, 2L, FILE_PROCESSING_STARTED);
        EnvelopeEvent event2 = event(uuid, halfHourAgo, 1L, DELETED);

        given(envelopeRepository.find(uuid)).willReturn(Optional.of(envelope));
        given(envelopeEventRepository.findForEnvelope(uuid))
            .willReturn(asList(event1, event2));

        // when
        // then
        assertThatThrownBy(() ->
                               envelopeActionService.rejectStaleEnvelope(uuid)
        )
            .isInstanceOf(EnvelopeCompletedOrNotStaleException.class)
            .hasMessageMatching("^(Envelope with id )[\\S]+( is completed or not stale)$");
    }

    @Test
    void should_throw_exception_if_no_events_for_envelope() {
        // given
        var uuid = UUID.randomUUID();
        var envelope = envelope(uuid, CREATED);

        given(envelopeRepository.find(uuid)).willReturn(Optional.of(envelope));

        // when
        // then
        assertThatThrownBy(() ->
                               envelopeActionService.rejectStaleEnvelope(uuid)
        )
            .isInstanceOf(NoSuchElementException.class);
    }

    @Test
    void should_throw_exception_if_envelope_has_dispatched_status() {
        // given
        var uuid = UUID.randomUUID();
        var envelope = envelope(uuid, DISPATCHED);

        given(envelopeRepository.find(uuid)).willReturn(Optional.of(envelope));

        // when
        // then
        assertThatThrownBy(() ->
                               envelopeActionService.rejectStaleEnvelope(uuid)
        )
            .isInstanceOf(EnvelopeCompletedOrNotStaleException.class)
            .hasMessageMatching("^(Envelope with id )[\\S]+( is completed or not stale)$");
    }

    @Test
    void should_throw_exception_if_envelope_has_rejected_status() {
        // given
        var uuid = UUID.randomUUID();
        var envelope = envelope(uuid, REJECTED);

        given(envelopeRepository.find(uuid)).willReturn(Optional.of(envelope));

        // when
        // then
        assertThatThrownBy(() ->
                               envelopeActionService.rejectStaleEnvelope(uuid)
        )
            .isInstanceOf(EnvelopeCompletedOrNotStaleException.class)
            .hasMessageMatching("^(Envelope with id )[\\S]+( is completed or not stale)$");
    }

    private Envelope envelope(UUID envelopeId, Status status) {
        return new Envelope(
            envelopeId,
            "container",
            "fileName.zip",
            Instant.now(),
            Instant.now(),
            Instant.now(),
            status,
            true,
            false
        );
    }

    private EnvelopeEvent event(UUID uuid, Instant threeHoursAgo, long l, EventType deleted) {
        return new EnvelopeEvent(
            l,
            uuid,
            deleted,
            null,
            null,
            threeHoursAgo
        );
    }
}
