package uk.gov.hmcts.reform.blobrouter.data;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
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
        eventRepo.insert(event1);
        eventRepo.insert(event2);

        var eventsInDb = eventRepo.findForEnvelope(envelopeId);

        // then
        assertThat(eventsInDb).hasSize(2);

        assertThat(eventsInDb.get(0).id).isNotNull();
        assertThat(eventsInDb.get(0).envelopeId).isEqualTo(event1.envelopeId);
        assertThat(eventsInDb.get(0).createdAt).isNotNull();
        assertThat(eventsInDb.get(0).notes).isEqualTo(event1.notes);

        assertThat(eventsInDb.get(1).id).isNotNull();
        assertThat(eventsInDb.get(1).envelopeId).isEqualTo(event2.envelopeId);
        assertThat(eventsInDb.get(1).createdAt).isNotNull();
        assertThat(eventsInDb.get(1).notes).isEqualTo(event2.notes);
    }

    @Test
    void should_throw_exception_when_trying_to_create_an_event_for_not_existing_envelope() {
        // given
        var event = new NewEnvelopeEvent(UUID.randomUUID(), EventType.REJECTED, "note");

        // when
        Throwable exc = catchThrowable(() -> eventRepo.insert(event));

        // then
        assertThat(exc).isNotNull();
    }

    @Test
    void should_return_empty_list_when_there_are_no_events_for_given_envelope() {
        assertThat(eventRepo.findForEnvelope(UUID.randomUUID())).isEmpty();
    }
}
