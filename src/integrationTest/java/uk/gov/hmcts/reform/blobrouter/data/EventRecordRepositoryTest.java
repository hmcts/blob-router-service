package uk.gov.hmcts.reform.blobrouter.data;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
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
    @MethodSource("events")
    void should_save_events_separately(NewEventRecord eventRecord) {
        // given
        long id = repo.insert(eventRecord);

        // when
        List<EventRecord> eventsInDb = repo.find(CONTAINER, FILE_NAME);

        // then
        assertThat(eventsInDb)
            .hasSize(1)
            .first()
            .satisfies(record -> {
                assertThat(record.id).isEqualTo(id);
                assertThat(record.event).isEqualTo(eventRecord.event);
                assertThat(record.notes).isEqualTo(eventRecord.notes);
            });
    }

    @Test
    void should_save_all_events_at_once() {
        // given
        insertEvents();

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

    @Test
    void should_not_find_events_when_one_of_search_params_mismatch() {
        // given
        insertEvents();

        // when
        List<EventRecord> eventsInDb1 = repo.find("NOPE" + CONTAINER, FILE_NAME);
        List<EventRecord> eventsInDb2 = repo.find(CONTAINER, "NOPE" + FILE_NAME);

        // then
        assertThat(eventsInDb1).isEmpty();
        assertThat(eventsInDb2).isEmpty();
    }

    private static Stream<Arguments> events() {
        return Stream.of(
            Arguments.of(new NewEventRecord(CONTAINER, FILE_NAME, FILE_RECEIVED)),
            Arguments.of(new NewEventRecord(CONTAINER, FILE_NAME, DISPATCHED)),
            Arguments.of(new NewEventRecord(CONTAINER, FILE_NAME, REJECTED, "description"))
        );
    }

    private void insertEvents() {
        events()
            .map(argument -> (NewEventRecord) argument.get()[0])
            .forEach(repo::insert);
    }
}
