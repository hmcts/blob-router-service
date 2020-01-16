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
public class EnvelopeRepositoryImplTest {

    @Autowired private EnvelopeRepositoryImpl repo;
    @Autowired private DbHelper dbHelper;

    @BeforeEach
    void setUp() {
        dbHelper.deleteAll();
    }

    @Test
    void should_save_and_read_envelope_by_id() {
        // given
        NewEnvelope newEnvelope = new NewEnvelope(
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
    void should_mark_existing_envelope_as_deleted() {
        // given
        NewEnvelope newEnvelope = new NewEnvelope(
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
        repo.insert(newEnvelope(Status.DISPATCHED));
        repo.insert(newEnvelope(Status.DISPATCHED));
        repo.insert(newEnvelope(Status.DISPATCHED));
        repo.insert(newEnvelope(Status.REJECTED));
        repo.insert(newEnvelope(Status.REJECTED));

        // when
        List<Envelope> dispatched = repo.find(Status.DISPATCHED, false);
        List<Envelope> rejected = repo.find(Status.REJECTED, false);

        // then
        assertThat(dispatched).hasSize(3);
        assertThat(rejected).hasSize(2);
    }

    @Test
    void should_find_deleted_envelopes_by_status() {
        // given
        NewEnvelope envelope = newEnvelope(Status.DISPATCHED);
        UUID id = repo.insert(envelope);
        repo.markAsDeleted(id);

        // when
        List<Envelope> deleted = repo.find(Status.DISPATCHED, true);
        List<Envelope> notDeleted = repo.find(Status.DISPATCHED, false);

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
    void should_find_envelopes_by_container_and_file_name() {
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
    void should_return_empty_optional_when_envelope_for_given_container_and_file_name_does_not_exist() {
        // given
        repo.insert(new NewEnvelope("a", "b", now(), now(), Status.DISPATCHED));

        // when
        Optional<Envelope> result = repo.find("some_other_file_name", "some_other_container");

        // then
        assertThat(result).isEmpty();
    }

    private NewEnvelope newEnvelope(Status status) {
        return new NewEnvelope(
            "container",
            UUID.randomUUID().toString(),
            now(),
            now().plusSeconds(100),
            status
        );
    }
}
