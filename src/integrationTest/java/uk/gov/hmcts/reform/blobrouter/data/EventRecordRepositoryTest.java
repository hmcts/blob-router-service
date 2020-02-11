package uk.gov.hmcts.reform.blobrouter.data;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import uk.gov.hmcts.reform.blobrouter.data.model.Event;
import uk.gov.hmcts.reform.blobrouter.data.model.EventRecord;
import uk.gov.hmcts.reform.blobrouter.data.model.NewEventRecord;

import java.util.List;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;
import static uk.gov.hmcts.reform.blobrouter.data.model.Event.DISPATCHED;
import static uk.gov.hmcts.reform.blobrouter.data.model.Event.FILE_RECEIVED;
import static uk.gov.hmcts.reform.blobrouter.data.model.Event.REJECTED;

@ActiveProfiles({"integration-test", "db-test"})
@SpringBootTest
public class EventRecordRepositoryTest {

    private static final String CONTAINER = "container";
    private static final String FILE_NAME = "file_name";

    @Autowired private EventRecordRepository repo;
    @Autowired private DbHelper dbHelper;

    @BeforeEach
    void setUp() {
        dbHelper.deleteAll();
    }

    @ParameterizedTest
    @MethodSource("eventWithNotes")
    void should_save_events_separately(Event event, String notes) {
        // given
        var newEnvelope = notes == null
            ? new NewEventRecord(CONTAINER, FILE_NAME, event)
            : new NewEventRecord(CONTAINER, FILE_NAME, event, notes);

        // when
        long id = repo.insert(newEnvelope);

        // and
        List<EventRecord> eventsInDb = repo.find(CONTAINER, FILE_NAME);

        // then
        assertThat(eventsInDb)
            .hasSize(1)
            .first()
            .satisfies(record -> {
                assertThat(record.id).isEqualTo(id);
                assertThat(record.event).isEqualTo(event);
                assertThat(record.notes).isEqualTo(notes);
            });
    }

    @Test
    void should_save_all_events_at_once() {
        // given
        eventWithNotes()
            .map(argument -> {
                Event event = (Event) argument.get()[0];
                String notes = (String) argument.get()[1];
                return new NewEventRecord(CONTAINER, FILE_NAME, event, notes);
            })
            .forEach(repo::insert);

        // when
        List<EventRecord> eventsInDb = repo.find(CONTAINER, FILE_NAME);

        // then
        assertThat(eventsInDb)
            .hasSize(3)
            .extracting(record -> tuple(record.event, record.notes))
            .containsExactlyInAnyOrder(
                tuple(FILE_RECEIVED, null),
                tuple(DISPATCHED, null),
                tuple(REJECTED, "description")
            );
    }

    private static Stream<Arguments> eventWithNotes() {
        return Stream.of(
            Arguments.of(FILE_RECEIVED, null),
            Arguments.of(DISPATCHED, null),
            Arguments.of(REJECTED, "description")
        );
    }
}
