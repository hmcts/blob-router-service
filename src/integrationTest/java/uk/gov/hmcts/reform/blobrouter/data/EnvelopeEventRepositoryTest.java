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
import uk.gov.hmcts.reform.blobrouter.data.events.EnvelopeEventRepository;
import uk.gov.hmcts.reform.blobrouter.data.events.EventType;
import uk.gov.hmcts.reform.blobrouter.data.events.NewEnvelopeEvent;

import java.util.UUID;

import static java.time.Instant.now;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.assertj.core.api.Assertions.tuple;

@ActiveProfiles({"integration-test", "db-test"})
@SpringBootTest
public class EnvelopeEventRepositoryTest {

    @Autowired EnvelopeRepository envelopeRepo;
    @Autowired DbHelper dbHelper;

    @Autowired EnvelopeEventRepository eventRepo;

    @AfterEach
    void tearDown() {
        dbHelper.deleteAll();
    }

    @Test
    void should_save_and_read_events() {
        // given
        var envelopeId = envelopeRepo.insert(new NewEnvelope("c", "f", now(), null, Status.CREATED));

        var event1 = new NewEnvelopeEvent(envelopeId, EventType.REJECTED, "note 1");
        var event2 = new NewEnvelopeEvent(envelopeId, EventType.DELETED, "note 2");

        // when
        long eventId1 = eventRepo.insert(event1);
        long eventId2 = eventRepo.insert(event2);

        var eventsInDb = eventRepo.findForEnvelope(envelopeId);

        // then
        assertThat(eventsInDb)
            .hasSize(2)
            .extracting(e -> tuple(e.id, e.envelopeId, e.type, e.notes))
            .containsExactlyInAnyOrder(
                tuple(eventId1, envelopeId, event1.type, event1.notes),
                tuple(eventId2, envelopeId, event2.type, event2.notes)
            );

        assertThat(eventsInDb.get(0).createdAt).isNotNull();
        assertThat(eventsInDb.get(1).createdAt).isNotNull();
    }

    @Test
    void should_throw_exception_when_trying_to_create_an_event_for_not_existing_envelope() {
        // given
        var event = new NewEnvelopeEvent(UUID.randomUUID(), EventType.REJECTED, "note");

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
}
