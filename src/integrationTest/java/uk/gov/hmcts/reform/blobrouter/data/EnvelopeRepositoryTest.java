package uk.gov.hmcts.reform.blobrouter.data;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import uk.gov.hmcts.reform.blobrouter.data.model.Envelope;
import uk.gov.hmcts.reform.blobrouter.data.model.NewEnvelope;
import uk.gov.hmcts.reform.blobrouter.data.model.Status;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static java.time.Instant.now;
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
    void should_find_single_envelope_by_container_and_file_name() {
        // given
        final String fileName = "foo.bar";
        final String container = "bar";

        // and
        repo.insert(new NewEnvelope(container, fileName, now(), now(), Status.DISPATCHED));

        // when
        Optional<Envelope> result = repo.find(fileName, container);

        // then
        assertThat(result).hasValueSatisfying(envelope -> {
            assertThat(envelope.fileName).isEqualTo(fileName);
            assertThat(envelope.container).isEqualTo(container);
        });
    }

    @Test
    void should_find_last_envelope_by_container_and_file_name() {
        // given
        final String fileName = "foo.bar";
        final String container = "bar";

        // and
        repo.insert(new NewEnvelope(container, fileName, now(), now(), Status.DISPATCHED));
        // this envelope should be selected
        UUID envelopeId2 =  repo.insert(
            new NewEnvelope(container, fileName, now().plusMillis(10000L), now(), Status.REJECTED)
        );
        new NewEnvelope(container, fileName, now().plusMillis(5000L), now(), Status.REJECTED);

        // when
        Optional<Envelope> result = repo.find(fileName, container);

        // then
        assertThat(result).hasValueSatisfying(envelope -> {
            assertThat(envelope.id).isEqualTo(envelopeId2);
            assertThat(envelope.fileName).isEqualTo(fileName);
            assertThat(envelope.container).isEqualTo(container);
        });
    }

    @Test
    void should_return_empty_optional_when_envelope_for_given_container_and_file_name_does_not_exist() {
        // given
        repo.insert(new NewEnvelope("a", "b", now(), now(), Status.DISPATCHED));

        // when
        Optional<Envelope> result = repo.find("some_other_file_name", "some_other_container");

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
        assertThat(repo.find(Status.DISPATCHED, false)).extracting(env -> env.fileName).containsExactly("f1", "f5");
        assertThat(repo.find(Status.DISPATCHED, true)).extracting(env -> env.fileName).containsExactly("f3", "f7");
        assertThat(repo.find(Status.REJECTED, false)).extracting(env -> env.fileName).containsExactly("f2", "f6");
        assertThat(repo.find(Status.REJECTED, true)).extracting(env -> env.fileName).containsExactly("f4", "f8");
    }

    private void addEnvelope(String container, String fileName, Status status, boolean isDeleted) {
        UUID id = repo.insert(new NewEnvelope(container, fileName, now(), now(), status));
        if (isDeleted) {
            repo.markAsDeleted(id);
        }
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
