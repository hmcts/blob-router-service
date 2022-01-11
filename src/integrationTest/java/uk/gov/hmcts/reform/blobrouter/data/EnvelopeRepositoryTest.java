package uk.gov.hmcts.reform.blobrouter.data;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.support.rowset.SqlRowSet;
import org.springframework.test.context.ActiveProfiles;
import uk.gov.hmcts.reform.blobrouter.data.envelopes.Envelope;
import uk.gov.hmcts.reform.blobrouter.data.envelopes.EnvelopeRepository;
import uk.gov.hmcts.reform.blobrouter.data.envelopes.NewEnvelope;
import uk.gov.hmcts.reform.blobrouter.data.envelopes.Status;

import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static com.google.common.collect.Sets.newHashSet;
import static java.time.Instant.now;
import static java.time.temporal.ChronoUnit.MINUTES;
import static java.util.Collections.emptyMap;
import static org.assertj.core.api.Assertions.assertThat;
import static uk.gov.hmcts.reform.blobrouter.data.envelopes.Status.CREATED;
import static uk.gov.hmcts.reform.blobrouter.data.envelopes.Status.DISPATCHED;
import static uk.gov.hmcts.reform.blobrouter.data.envelopes.Status.REJECTED;

@ActiveProfiles({"integration-test", "db-test"})
@SpringBootTest
public class EnvelopeRepositoryTest {

    @Autowired private EnvelopeRepository repo;
    @Autowired private DbHelper dbHelper;
    @Autowired private NamedParameterJdbcTemplate jdbcTemplate;

    @BeforeEach
    void setUp() {
        dbHelper.deleteAll();
    }

    @Test
    void should_save_and_read_envelope_by_id() {
        // given
        var newEnvelope = new NewEnvelope(
            "container",
            "hello.zip",
            now(),
            now().plusSeconds(100),
            DISPATCHED,
            1024L
        );

        // when
        UUID id = repo.insert(newEnvelope);

        // and
        Optional<Envelope> envelopeInDb = repo.find(id);

        // then
        assertThat(envelopeInDb).hasValueSatisfying(env -> {
            assertThat(env.container).isEqualTo(newEnvelope.container);
            assertThat(env.fileName).isEqualTo(newEnvelope.fileName);
            assertThat(env.dispatchedAt).isEqualTo(newEnvelope.dispatchedAt);
            assertThat(env.fileCreatedAt).isEqualTo(newEnvelope.fileCreatedAt);
            assertThat(env.status).isEqualTo(newEnvelope.status);
            assertThat(env.isDeleted).isEqualTo(false);
            assertThat(env.createdAt).isNotNull();
            assertThat(env.fileSize).isEqualTo(1024L);
        });
    }

    @Test
    void should_handle_null_file_size() {
        // given
        var newEnvelope = new NewEnvelope(
            "container",
            "hello.zip",
            now(),
            now().plusSeconds(100),
            DISPATCHED,
            null
        );

        // when
        UUID id = repo.insert(newEnvelope);

        // and
        SqlRowSet rs = jdbcTemplate.queryForRowSet("SELECT * FROM envelopes WHERE id='" + id + "'", emptyMap());

        // then
        rs.first();
        assertThat(rs.wasNull()).isFalse();
        long fileSize = rs.getLong("file_size");
        assertThat(rs.wasNull()).isTrue();
        assertThat(fileSize).isZero();
    }

    @Test
    void should_handle_not_null_file_size() {
        // given
        var newEnvelope = new NewEnvelope(
            "container",
            "hello.zip",
            now(),
            now().plusSeconds(100),
            DISPATCHED,
            1024L
        );

        // when
        UUID id = repo.insert(newEnvelope);

        // and
        SqlRowSet rs = jdbcTemplate.queryForRowSet("SELECT * FROM envelopes WHERE id='" + id + "'", emptyMap());

        // then
        rs.first();
        assertThat(rs.wasNull()).isFalse();
        assertThat(rs.getLong("file_size")).isEqualTo(1024L);
    }

    @Test
    void should_handle_not_dispatched_envelope() {
        // given
        var newEnvelope = new NewEnvelope(
            "container",
            "hello.zip",
            now(),
            null,
            REJECTED,
            1024L
        );

        // when
        UUID id = repo.insert(newEnvelope);

        // and
        Optional<Envelope> envelopeInDb = repo.find(id);

        // then
        assertThat(envelopeInDb).hasValueSatisfying(env -> {
            assertThat(env.container).isEqualTo(newEnvelope.container);
            assertThat(env.fileName).isEqualTo(newEnvelope.fileName);
            assertThat(env.dispatchedAt).isNull();
            assertThat(env.fileCreatedAt).isEqualTo(newEnvelope.fileCreatedAt);
            assertThat(env.status).isEqualTo(newEnvelope.status);
            assertThat(env.isDeleted).isEqualTo(false);
            assertThat(env.createdAt).isNotNull();
            assertThat(env.fileSize).isEqualTo(1024L);
        });
    }

    @Test
    void should_mark_existing_envelope_as_deleted() {
        // given
        var newEnvelope = new NewEnvelope(
            "container",
            "hello.zip",
            now(),
            now().plusSeconds(100),
            DISPATCHED,
            1024L
        );

        UUID id = repo.insert(newEnvelope);

        // when
        int updateCount = repo.markAsDeleted(id);
        Optional<Envelope> envelopeAfterUpdate = repo.find(id);

        // then
        assertThat(updateCount).isEqualTo(1);
        assertThat(envelopeAfterUpdate).hasValueSatisfying(env -> assertThat(env.isDeleted).isEqualTo(true));
    }

    @Test
    void should_update_envelope_as_notification_sent() {
        // given
        var newEnvelope = new NewEnvelope(
            "container",
            "hello.zip",
            now(),
            now().plusSeconds(100),
            REJECTED,
            1024L
        );

        UUID id = repo.insert(newEnvelope);

        // when
        int updateCount = repo.updatePendingNotification(id, false);
        Optional<Envelope> envelopeAfterUpdate = repo.find(id);

        // then
        assertThat(updateCount).isEqualTo(1);
        assertThat(envelopeAfterUpdate).hasValueSatisfying(env -> assertThat(env.pendingNotification).isEqualTo(false));
    }

    @Test
    void should_update_envelopes_status() {
        // given
        var oldStatus = DISPATCHED;
        var newStatus = REJECTED;

        UUID id = repo.insert(new NewEnvelope("a", "b", now(), null, oldStatus, 1024L));

        // when
        repo.updateStatus(id, newStatus);

        // then
        assertThat(repo.find(id))
            .hasValueSatisfying(env -> assertThat(env.status).isEqualTo(newStatus));
    }

    @Test
    void should_update_dispatch_time() {
        // given
        UUID id = repo.insert(new NewEnvelope("a", "b", now(), null, DISPATCHED, 1024L));
        Instant newDispatchTime = now();

        // when
        repo.updateDispatchDateTime(id, newDispatchTime);

        // then
        assertThat(repo.find(id))
            .hasValueSatisfying(env -> assertThat(env.dispatchedAt).isEqualTo(newDispatchTime));
    }

    @Test
    void should_return_zero_if_no_envelopes_were_marked_as_deleted() {
        // given no envelopes in DB

        // when
        int updateCount = repo.markAsDeleted(UUID.randomUUID());

        // then
        assertThat(updateCount).isEqualTo(0);
    }

    @Test
    void should_find_not_deleted_envelopes_by_status() {
        // given
        String container = "container1";
        repo.insert(newEnvelope(DISPATCHED, container));
        repo.insert(newEnvelope(DISPATCHED, container));
        repo.insert(newEnvelope(DISPATCHED, container));
        repo.insert(newEnvelope(REJECTED, container));
        repo.insert(newEnvelope(REJECTED, container));

        // when
        List<Envelope> dispatched = repo.find(DISPATCHED, container, false);
        List<Envelope> rejected = repo.find(REJECTED, container, false);

        // then
        assertThat(dispatched).hasSize(3);
        assertThat(rejected).hasSize(2);
    }

    @Test
    void should_find_envelopes_only_for_the_requested_container() {
        // given
        String containerName = "container1";
        repo.insert(newEnvelope(DISPATCHED, containerName));
        repo.insert(newEnvelope(DISPATCHED, "other-container"));

        // when
        List<Envelope> found = repo.find(DISPATCHED, containerName, false);

        // then
        assertThat(found).hasSize(1);
        assertThat(found.get(0).container).isEqualTo(containerName);
    }

    @Test
    void should_find_deleted_envelopes_by_status() {
        // given
        String containerName = "container1";
        NewEnvelope envelope = newEnvelope(DISPATCHED, containerName);
        UUID id = repo.insert(envelope);
        repo.markAsDeleted(id);

        // when
        List<Envelope> deleted = repo.find(DISPATCHED, containerName, true);
        List<Envelope> notDeleted = repo.find(DISPATCHED, containerName, false);

        // then
        assertThat(deleted).hasSize(1);
        assertThat(notDeleted).hasSize(0);

        assertThat(deleted).hasOnlyOneElementSatisfying(env -> {
            assertThat(env.id).isEqualTo(id);
            assertThat(env.container).isEqualTo(envelope.container);
            assertThat(env.fileName).isEqualTo(envelope.fileName);
            assertThat(env.dispatchedAt).isEqualTo(envelope.dispatchedAt);
            assertThat(env.fileCreatedAt).isEqualTo(envelope.fileCreatedAt);
            assertThat(env.status).isEqualTo(envelope.status);
            assertThat(env.isDeleted).isEqualTo(true);
            assertThat(env.createdAt).isNotNull();
            assertThat(env.fileSize).isEqualTo(1024L);
        });
    }

    @Test
    void should_find_last_envelope_by_container_and_file_name() {
        // given
        final String fileName = "foo.bar";
        final String container = "bar";

        // and
        repo.insert(new NewEnvelope(container, fileName, now().minusSeconds(99), now(), DISPATCHED, null));
        repo.insert(new NewEnvelope(container, fileName, now().minusSeconds(10), null, REJECTED, null));

        // when
        Optional<Envelope> result = repo.findLast(fileName, container);

        // then
        assertThat(result).hasValueSatisfying(envelope -> {
            assertThat(envelope.fileName).isEqualTo(fileName);
            assertThat(envelope.container).isEqualTo(container);
            assertThat(envelope.status).isEqualTo(REJECTED);
        });
    }

    @Test
    void should_return_empty_optional_when_last_envelope_for_given_container_and_file_name_does_not_exist() {
        // given
        repo.insert(new NewEnvelope("a", "b", now(), now(), DISPATCHED, null));

        // when
        Optional<Envelope> result = repo.findLast("some_other_file_name", "some_other_container");

        // then
        assertThat(result).isEmpty();
    }

    @Test
    void should_find_envelopes_by_status() {
        //given
        addEnvelope("C1", "f1", DISPATCHED, false);
        addEnvelope("C1", "f2", REJECTED, false);
        addEnvelope("C1", "f3", DISPATCHED, true);
        addEnvelope("C1", "f4", REJECTED, true);

        addEnvelope("C2", "f5", DISPATCHED, false);
        addEnvelope("C2", "f6", REJECTED, false);
        addEnvelope("C2", "f7", DISPATCHED, true);
        addEnvelope("C2", "f8", REJECTED, true);

        // then
        assertThat(repo.find(DISPATCHED, false))
            .extracting(env -> env.fileName)
            .containsExactlyInAnyOrder("f1", "f5");
        assertThat(repo.find(DISPATCHED, true))
            .extracting(env -> env.fileName)
            .containsExactlyInAnyOrder("f3", "f7");
        assertThat(repo.find(REJECTED, false))
            .extracting(env -> env.fileName)
            .containsExactlyInAnyOrder("f2", "f6");
        assertThat(repo.find(REJECTED, true))
            .extracting(env -> env.fileName)
            .containsExactlyInAnyOrder("f4", "f8");
    }

    @Test
    void should_find_envelopes_by_file_name_and_container() {
        //given
        var id1 = addEnvelope("xxx", "C1");
        var id2 = addEnvelope("xxx", "C1");
        var id3 = addEnvelope("yyy", "C1");
        var id4 = addEnvelope("xxx", "C2");

        // then
        assertThat(repo.find("xxx", "C1")).extracting(env -> env.id).containsExactlyInAnyOrder(id1, id2);
        assertThat(repo.find("yyy", "C1")).extracting(env -> env.id).containsExactly(id3);
        assertThat(repo.find("xxx", "C2")).extracting(env -> env.id).containsExactly(id4);
        assertThat(repo.find("aaa", "C1")).isEmpty();
    }

    @Test
    void should_return_envelopes_count_for_container_and_time_range() {
        //given
        Instant fromDate = now().minus(5, MINUTES);

        addEnvelope("C1", "f1", CREATED, fromDate.plusSeconds(60)); // in time range
        addEnvelope("C3", "f3", DISPATCHED, fromDate.plusSeconds(10)); // in time range
        addEnvelope("C2", "f2", REJECTED, fromDate.minusSeconds(30)); // not in time range
        addEnvelope("some-container", "f4", CREATED, fromDate.plusSeconds(30)); // invalid container

        // then
        assertThat(repo.getEnvelopesCount(newHashSet("C1", "C2", "C3", "C4"), fromDate, now())).isEqualTo(2);
    }

    @Test
    void should_return_envelopes_for_the_requested_date() {
        //given
        UUID id1 = addEnvelope("f1", "C1");
        UUID id2 = addEnvelope("f2", "C2");
        UUID id3 = addEnvelope("f3", "C2");

        // when
        List<Envelope> envelopes = repo.findEnvelopes(null, null, LocalDate.now());

        // then
        assertThat(envelopes).isNotEmpty().hasSize(3);
        assertThat(envelopes).extracting(env -> env.id).containsExactly(id3, id2, id1); // ordered by created_at
    }

    @Test
    void should_return_envelopes_for_the_requested_date_and_container() {
        //given
        addEnvelope("f1", "C1");
        addEnvelope("f2", "C1");
        addEnvelope("f3", "C2");

        // when
        List<Envelope> envelopes = repo.findEnvelopes(null, "C2", LocalDate.now());

        // then
        assertThat(envelopes).isNotEmpty().hasSize(1);
        assertThat(envelopes.get(0).container).isEqualTo("C2");
        assertThat(envelopes.get(0).fileName).isEqualTo("f3");
    }

    @Test
    void should_return_envelopes_for_the_requested_date_container_and_filename() {
        //given
        addEnvelope("f1", "C1");
        addEnvelope("f2", "C1");
        addEnvelope("f3", "C1");

        // when
        List<Envelope> envelopes = repo.findEnvelopes("f1", "C1", LocalDate.now());

        // then
        assertThat(envelopes).isNotEmpty().hasSize(1);
        assertThat(envelopes.get(0).container).isEqualTo("C1");
        assertThat(envelopes.get(0).fileName).isEqualTo("f1");
    }

    @Test
    void should_return_all_envelopes_when_all_params_are_null() {
        //given
        UUID id1 = addEnvelope("f1", "C1");
        UUID id2 = addEnvelope("f2", "C2");
        UUID id3 = addEnvelope("f3", "C2");

        // when
        List<Envelope> envelopes = repo.findEnvelopes(null, null, null);

        // then
        assertThat(envelopes).isNotEmpty().hasSize(3);
        assertThat(envelopes).extracting(env -> env.id).containsExactly(id3, id2, id1); // ordered by created_at
    }

    @Test
    void should_return_empty_list_when_no_envelopes_exist_for_the_requested_date() {
        //given
        addEnvelope("C1", "f1");
        addEnvelope("C2", "f2");

        // when
        List<Envelope> envelopes = repo.findEnvelopes(
            null, null, LocalDate.now().minusDays(1)
        ); //query for previous day

        // then
        assertThat(envelopes).isEmpty();
    }

    @Test
    void should_get_empty_result_when_no_incomplete_envelopes_are_there_in_db() {
        assertThat(repo.getIncompleteEnvelopesBefore(now().minusSeconds(3600))).isEmpty();
    }

    @Test
    void should_get_incomplete_envelopes() {
        // given
        repo.insert(new NewEnvelope("X", "A.zip", now().minusSeconds(7200), null, CREATED, null));
        repo.insert(new NewEnvelope("Y", "B.zip", now().minusSeconds(10), null, DISPATCHED, null));
        repo.insert(new NewEnvelope("Z", "C.zip", now().minusSeconds(10), null, REJECTED, null));
        repo.insert(new NewEnvelope("Z", "D.zip", now().minusSeconds(7200), null, CREATED, null));
        repo.insert(new NewEnvelope("Z", "E.zip", now().minusSeconds(10), null, CREATED, null));

        // when
        List<Envelope> result = repo.getIncompleteEnvelopesBefore(now().minusSeconds(3600));

        // then
        assertThat(result)
            .extracting(envelope -> envelope.fileName)
            .containsExactlyInAnyOrder("A.zip", "D.zip");
    }

    @Test
    void should_return_empty_when_envelope_status_is_created() {
        // given
        repo.insert(new NewEnvelope("X", "A.zip", now().minusSeconds(10), null, CREATED, null));
        repo.insert(new NewEnvelope("X", "A.zip", now().minusSeconds(10), null, CREATED, null));
        repo.insert(new NewEnvelope("Y", "A.zip", now().minusSeconds(7200), null, REJECTED, null));
        repo.insert(new NewEnvelope("X", "E.zip", now().minusSeconds(10), null, DISPATCHED, null));

        // when
        Optional<Envelope> envelopeInDb = repo.findEnvelopeNotInCreatedStatus("A.zip", "X");

        // then
        assertThat(envelopeInDb).isEmpty();
    }

    @Test
    void should_get_envelope_not_in_created_status() {
        // given
        NewEnvelope newEnvelope =
            new NewEnvelope("X", "A.zip", now().minusSeconds(7200), null, DISPATCHED, null);
        repo.insert(newEnvelope);
        repo.insert(new NewEnvelope("X", "B.zip", now().minusSeconds(10), null, DISPATCHED, null));
        repo.insert(new NewEnvelope("X", "C.zip", now().minusSeconds(10), null, REJECTED, null));
        repo.insert(new NewEnvelope("X", "A.zip", now().minusSeconds(100), null, CREATED, null));
        repo.insert(new NewEnvelope("X", "E.zip", now().minusSeconds(10), null, CREATED, null));

        // when
        Optional<Envelope> envelopeInDb = repo.findEnvelopeNotInCreatedStatus("A.zip", "X");

        // then
        assertThat(envelopeInDb).hasValueSatisfying(env -> {
            assertThat(env.container).isEqualTo(newEnvelope.container);
            assertThat(env.fileName).isEqualTo(newEnvelope.fileName);
            assertThat(env.dispatchedAt).isEqualTo(newEnvelope.dispatchedAt);
            assertThat(env.fileCreatedAt).isEqualTo(newEnvelope.fileCreatedAt);
            assertThat(env.status).isEqualTo(newEnvelope.status);
            assertThat(env.isDeleted).isEqualTo(false);
            assertThat(env.createdAt).isNotNull();
        });
    }


    @Test
    void should_return_envelopes_by_dcn() {
        // given
        repo.insert(new NewEnvelope("X", "2313131.zip", now(), null, CREATED, null));
        repo.insert(new NewEnvelope("Y", "231313.zip", now().minus(2, ChronoUnit.DAYS), null, DISPATCHED, null));
        repo.insert(new NewEnvelope("F", "231313_x.zip", now(), null, REJECTED, null));
        repo.insert(new NewEnvelope("K", "231313_b.zip", now().minus(3, ChronoUnit.DAYS), null, CREATED, null));
        repo.insert(new NewEnvelope("B", "41419090.zip", now(), null, CREATED, null));

        // when
        List<Envelope> result = repo.findEnvelopesByDcnPrefix("231313", LocalDate.now().minusDays(1),LocalDate.now());

        // then
        assertThat(result)
            .extracting(envelope -> envelope.fileName)
            .containsExactlyInAnyOrder("231313_x.zip", "2313131.zip");
    }

    @Test
    void should_return_empty_list_when_no_envelopes_matching_by_dcn() {
        // given
        repo.insert(new NewEnvelope("X", "2313131.zip", now().minus(4, ChronoUnit.DAYS), null, CREATED, null));
        repo.insert(new NewEnvelope("Y", "231313.zip", now().minus(2, ChronoUnit.DAYS), null, DISPATCHED, null));
        repo.insert(new NewEnvelope("F", "23131_x.zip", now(), null, REJECTED, null));
        repo.insert(new NewEnvelope("K", "231313_b.zip", now().minus(3, ChronoUnit.DAYS), null, CREATED, null));
        repo.insert(new NewEnvelope("B", "41419090.zip", now(), null, CREATED, null));

        // when
        List<Envelope> result = repo.findEnvelopesByDcnPrefix("231313", LocalDate.now().minusDays(1), LocalDate.now());

        // then
        assertThat(result).isEmpty();
    }

    private UUID addEnvelope(String fileName, String container) {
        return addEnvelope(container, fileName, CREATED, false);
    }

    private UUID addEnvelope(String container, String fileName, Status status, boolean isDeleted) {
        UUID id = repo.insert(new NewEnvelope(container, fileName, now(), now(), status, null));
        if (isDeleted) {
            repo.markAsDeleted(id);
        }
        return id;
    }

    private void addEnvelope(String container, String fileName, Status status, Instant fileCreatedAt) {
        repo.insert(
            new NewEnvelope(
                container,
                fileName,
                fileCreatedAt,
                fileCreatedAt.plusSeconds(100),
                status,
                null
            )
        );
    }

    private NewEnvelope newEnvelope(Status status, String container) {
        return new NewEnvelope(
            container,
            UUID.randomUUID().toString(),
            now(),
            now().plusSeconds(100),
            status,
            1024L
        );
    }
}
