package uk.gov.hmcts.reform.blobrouter.data;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import uk.gov.hmcts.reform.blobrouter.data.envelopes.Envelope;
import uk.gov.hmcts.reform.blobrouter.data.envelopes.EnvelopeRepository;
import uk.gov.hmcts.reform.blobrouter.data.envelopes.NewEnvelope;
import uk.gov.hmcts.reform.blobrouter.data.envelopes.Status;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static com.google.common.collect.Sets.newHashSet;
import static java.time.Instant.now;
import static java.time.temporal.ChronoUnit.MINUTES;
import static org.assertj.core.api.Assertions.assertThat;

@ActiveProfiles({"integration-test", "db-test"})
@SpringBootTest
public class EnvelopeRepositoryTest {

    @Autowired private EnvelopeRepository repo;
    @Autowired private DbHelper dbHelper;

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
            Status.DISPATCHED
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
        });
    }

    @Test
    void should_handle_not_dispatched_envelope() {
        // given
        var newEnvelope = new NewEnvelope(
            "container",
            "hello.zip",
            now(),
            null,
            Status.REJECTED
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
            Status.DISPATCHED
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
            Status.REJECTED
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
        var oldStatus = Status.DISPATCHED;
        var newStatus = Status.REJECTED;

        UUID id = repo.insert(new NewEnvelope("a", "b", now(), null, oldStatus));

        // when
        repo.updateStatus(id, newStatus);

        // then
        assertThat(repo.find(id))
            .hasValueSatisfying(env -> assertThat(env.status).isEqualTo(newStatus));
    }

    @Test
    void should_update_dispatch_time() {
        // given
        UUID id = repo.insert(new NewEnvelope("a", "b", now(), null, Status.DISPATCHED));
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
        repo.insert(newEnvelope(Status.DISPATCHED, container));
        repo.insert(newEnvelope(Status.DISPATCHED, container));
        repo.insert(newEnvelope(Status.DISPATCHED, container));
        repo.insert(newEnvelope(Status.REJECTED, container));
        repo.insert(newEnvelope(Status.REJECTED, container));

        // when
        List<Envelope> dispatched = repo.find(Status.DISPATCHED, container, false);
        List<Envelope> rejected = repo.find(Status.REJECTED, container, false);

        // then
        assertThat(dispatched).hasSize(3);
        assertThat(rejected).hasSize(2);
    }

    @Test
    void should_find_envelopes_only_for_the_requested_container() {
        // given
        String containerName = "container1";
        repo.insert(newEnvelope(Status.DISPATCHED, containerName));
        repo.insert(newEnvelope(Status.DISPATCHED, "other-container"));

        // when
        List<Envelope> found = repo.find(Status.DISPATCHED, containerName, false);

        // then
        assertThat(found).hasSize(1);
        assertThat(found.get(0).container).isEqualTo(containerName);
    }

    @Test
    void should_find_deleted_envelopes_by_status() {
        // given
        String containerName = "container1";
        NewEnvelope envelope = newEnvelope(Status.DISPATCHED, containerName);
        UUID id = repo.insert(envelope);
        repo.markAsDeleted(id);

        // when
        List<Envelope> deleted = repo.find(Status.DISPATCHED, containerName, true);
        List<Envelope> notDeleted = repo.find(Status.DISPATCHED, containerName, false);

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
        });
    }

    @Test
    void should_find_last_envelope_by_container_and_file_name() {
        // given
        final String fileName = "foo.bar";
        final String container = "bar";

        // and
        repo.insert(new NewEnvelope(container, fileName, now().minusSeconds(99), now(), Status.DISPATCHED));
        repo.insert(new NewEnvelope(container, fileName, now().minusSeconds(10), null, Status.REJECTED));

        // when
        Optional<Envelope> result = repo.findLast(fileName, container);

        // then
        assertThat(result).hasValueSatisfying(envelope -> {
            assertThat(envelope.fileName).isEqualTo(fileName);
            assertThat(envelope.container).isEqualTo(container);
            assertThat(envelope.status).isEqualTo(Status.REJECTED);
        });
    }

    @Test
    void should_return_empty_optional_when_last_envelope_for_given_container_and_file_name_does_not_exist() {
        // given
        repo.insert(new NewEnvelope("a", "b", now(), now(), Status.DISPATCHED));

        // when
        Optional<Envelope> result = repo.findLast("some_other_file_name", "some_other_container");

        // then
        assertThat(result).isEmpty();
    }

    @Test
    void should_find_envelopes_by_status() {
        //given
        addEnvelope("C1", "f1", Status.DISPATCHED, false);
        addEnvelope("C1", "f2", Status.REJECTED, false);
        addEnvelope("C1", "f3", Status.DISPATCHED, true);
        addEnvelope("C1", "f4", Status.REJECTED, true);

        addEnvelope("C2", "f5", Status.DISPATCHED, false);
        addEnvelope("C2", "f6", Status.REJECTED, false);
        addEnvelope("C2", "f7", Status.DISPATCHED, true);
        addEnvelope("C2", "f8", Status.REJECTED, true);

        // then
        assertThat(repo.find(Status.DISPATCHED, false))
            .extracting(env -> env.fileName)
            .containsExactlyInAnyOrder("f1", "f5");
        assertThat(repo.find(Status.DISPATCHED, true))
            .extracting(env -> env.fileName)
            .containsExactlyInAnyOrder("f3", "f7");
        assertThat(repo.find(Status.REJECTED, false))
            .extracting(env -> env.fileName)
            .containsExactlyInAnyOrder("f2", "f6");
        assertThat(repo.find(Status.REJECTED, true))
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

        addEnvelope("C1", "f1", Status.CREATED, fromDate.plusSeconds(60)); // in time range
        addEnvelope("C3", "f3", Status.DISPATCHED, fromDate.plusSeconds(10)); // in time range
        addEnvelope("C2", "f2", Status.REJECTED, fromDate.minusSeconds(30)); // not in time range
        addEnvelope("some-container", "f4", Status.CREATED, fromDate.plusSeconds(30)); // invalid container

        // then
        assertThat(repo.getEnvelopesCount(newHashSet("C1", "C2", "C3", "C4"), fromDate, now())).isEqualTo(2);
    }

    @Test
    void should_return_envelopes_for_the_requested_date() {
        //given
        addEnvelope("C1", "f1");
        addEnvelope("C3", "f2");
        addEnvelope("C3", "f3");

        // when
        List<Envelope> envelopes = repo.findEnvelopes(null, null, LocalDate.now());

        // then
        assertThat(envelopes).isNotEmpty().hasSize(3);
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
        addEnvelope("f1", "C1");
        addEnvelope("f2", "C2");
        addEnvelope("f3", "C2");

        // when
        List<Envelope> envelopes = repo.findEnvelopes(null, null, null);

        // then
        assertThat(envelopes).isNotEmpty().hasSize(3);
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

    private UUID addEnvelope(String fileName, String container) {
        return addEnvelope(container, fileName, Status.CREATED, false);
    }

    private UUID addEnvelope(String container, String fileName, Status status, boolean isDeleted) {
        UUID id = repo.insert(new NewEnvelope(container, fileName, now(), now(), status));
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
                status
            )
        );
    }

    private NewEnvelope newEnvelope(Status status, String container) {
        return new NewEnvelope(
            container,
            UUID.randomUUID().toString(),
            now(),
            now().plusSeconds(100),
            status
        );
    }
}
