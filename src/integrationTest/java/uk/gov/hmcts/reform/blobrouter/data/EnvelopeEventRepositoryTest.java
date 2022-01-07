package uk.gov.hmcts.reform.blobrouter.data;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.context.ActiveProfiles;
import uk.gov.hmcts.reform.blobrouter.data.envelopes.EnvelopeRepository;
import uk.gov.hmcts.reform.blobrouter.data.envelopes.NewEnvelope;
import uk.gov.hmcts.reform.blobrouter.data.envelopes.Status;
import uk.gov.hmcts.reform.blobrouter.data.events.EnvelopeEvent;
import uk.gov.hmcts.reform.blobrouter.data.events.EnvelopeEventRepository;
import uk.gov.hmcts.reform.blobrouter.data.events.ErrorCode;
import uk.gov.hmcts.reform.blobrouter.data.events.EventType;
import uk.gov.hmcts.reform.blobrouter.data.events.NewEnvelopeEvent;

import java.util.UUID;

import static java.time.Instant.now;
import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

@ActiveProfiles({"integration-test", "db-test"})
@SpringBootTest
public class EnvelopeEventRepositoryTest {

    @Autowired private EnvelopeRepository envelopeRepo;
    @Autowired private DbHelper dbHelper;

    @Autowired private EnvelopeEventRepository eventRepo;

    @AfterEach
    void tearDown() {
        dbHelper.deleteAll();
    }

    @Test
    void should_save_and_read_events() {
        // given
        var envelopeId = envelopeRepo.insert(new NewEnvelope("c", "f", now(), null, Status.CREATED, null));

        var event1 = new NewEnvelopeEvent(envelopeId, EventType.REJECTED, ErrorCode.ERR_SIG_VERIFY_FAILED, "note 1");
        var event2 = new NewEnvelopeEvent(envelopeId, EventType.DELETED, null, "note 2");

        // when
        long eventId1 = eventRepo.insert(event1);
        long eventId2 = eventRepo.insert(event2);

        var eventsInDb = eventRepo.findForEnvelope(envelopeId);

        // then
        assertThat(eventsInDb)
            .hasSize(2)
            .usingElementComparatorIgnoringFields("createdAt")
            .containsExactlyInAnyOrder(
                envelopeEvent(envelopeId, event1, eventId1),
                envelopeEvent(envelopeId, event2, eventId2)
            );

        assertThat(eventsInDb.get(0).createdAt).isNotNull();
        assertThat(eventsInDb.get(1).createdAt).isNotNull();
    }

    @Test
    void should_throw_exception_when_trying_to_create_an_event_for_not_existing_envelope() {
        // given
        var event = new NewEnvelopeEvent(UUID.randomUUID(), EventType.REJECTED, ErrorCode.ERR_AV_FAILED, "note");

        // when
        Throwable exc = catchThrowable(() -> eventRepo.insert(event));

        // then
        assertThat(exc)
            .isInstanceOf(DataIntegrityViolationException.class)
            .hasMessageContaining("foreign key");
    }

    @Test
    void should_return_empty_list_when_there_are_no_events_for_given_envelope() {
        assertThat(eventRepo.findForEnvelope(UUID.randomUUID())).isEmpty();
    }

    @Test
    void should_return_events_for_envelope_ids_order_by_created_at() {
        // given
        var envelopeId1 = envelopeRepo.insert(new NewEnvelope("c1", "f1", now(), null, Status.CREATED, null));
        var envelopeId2 = envelopeRepo.insert(new NewEnvelope("c2", "f2", now(), null, Status.DISPATCHED, null));

        var event1a = new NewEnvelopeEvent(envelopeId1, EventType.FILE_PROCESSING_STARTED, null, "note 1");
        var event2a = new NewEnvelopeEvent(envelopeId2, EventType.FILE_PROCESSING_STARTED, null, "note 3");
        var event2b = new NewEnvelopeEvent(envelopeId2, EventType.DISPATCHED, null, "note 4");

        // when
        var eventId1a = eventRepo.insert(event1a);
        var eventId2a = eventRepo.insert(event2a);
        var eventId2b = eventRepo.insert(event2b);

        var eventsInDb = eventRepo.findForEnvelopes(asList(envelopeId1, envelopeId2));

        // then
        assertThat(eventsInDb)
            .hasSize(3)
            .usingElementComparatorIgnoringFields("createdAt")
            .containsExactly(
                envelopeEvent(envelopeId2, event2b, eventId2b),
                envelopeEvent(envelopeId2, event2a, eventId2a),
                envelopeEvent(envelopeId1, event1a, eventId1a)
            );
    }

    @Test
    void should_return_empty_when_no_events_exist_for_envelopes() {
        // given
        var envelopeId1 = envelopeRepo.insert(new NewEnvelope("c1", "f1", now(), null, Status.CREATED, null));
        var envelopeId2 = envelopeRepo.insert(new NewEnvelope("c2", "f2", now(), null, Status.CREATED, null));

        // when
        var eventsInDb = eventRepo.findForEnvelopes(asList(envelopeId1, envelopeId2));

        // then
        assertThat(eventsInDb).isEmpty();
    }

    private EnvelopeEvent envelopeEvent(UUID envelopeId, NewEnvelopeEvent event, long eventId) {
        return new EnvelopeEvent(eventId, envelopeId, event.type, event.errorCode, event.notes, now());
    }

}
