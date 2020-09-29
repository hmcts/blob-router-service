package uk.gov.hmcts.reform.blobrouter.data;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import uk.gov.hmcts.reform.blobrouter.data.reconciliation.statements.SupplierStatementRepository;
import uk.gov.hmcts.reform.blobrouter.data.reconciliation.statements.model.EnvelopeSupplierStatement;
import uk.gov.hmcts.reform.blobrouter.data.reconciliation.statements.model.NewEnvelopeSupplierStatement;

import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;
import javax.validation.ClockProvider;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

@ActiveProfiles({"integration-test", "db-test"})
@SpringBootTest
public class SupplierStatementRepositoryTest {

    @Autowired private SupplierStatementRepository repo;
    @Autowired private ClockProvider clockProvider;

    @Autowired private NamedParameterJdbcTemplate jdbcTemplate;

    @BeforeEach
    void setUp() {
        jdbcTemplate
            .update("DELETE FROM envelope_supplier_statements", new MapSqlParameterSource());
    }

    @Test
    void should_save_and_find_statement() throws Exception {
        // given
        var statement =
            new NewEnvelopeSupplierStatement(
                LocalDate.now(),
                "{ \"a\": 100 }",
                "v1.0.0"
            );

        var start = LocalDateTime.now(clockProvider.getClock());

        // when
        UUID id = repo.save(statement);
        var statementInDb = repo.findById(id);

        var finish = LocalDateTime.now(clockProvider.getClock());

        // then
        assertThat(statementInDb).isNotEmpty();
        assertThat(statementInDb).hasValueSatisfying(s -> {
            assertThat(s.id).isEqualTo(id);
            assertThat(s.date).isEqualTo(statement.date);
            assertThat(s.content).isEqualTo(statement.content);
            assertThat(s.contentTypeVersion).isEqualTo(statement.contentTypeVersion);
            assertThat(s.createdAt).isNotNull();
            assertThat(s.createdAt).isAfter(start);
            assertThat(s.createdAt).isBefore(finish);
        });
    }

    @Test
    void should_throw_exception_if_invalid_json_is_passed() {
        // given
        var statement =
            new NewEnvelopeSupplierStatement(
                LocalDate.now(),
                "&ASD^AS^DAS^",
                "v1.0.0"
            );

        // when
        var exc = catchThrowable(() -> repo.save(statement));

        // then
        assertThat(exc).isInstanceOf(DataIntegrityViolationException.class);
        assertThat(exc.getMessage()).contains("invalid input syntax for type json");
    }

    @Test
    void should_return_empty_optional_if_statement_does_not_exist() {
        // given
        UUID uuid = UUID.randomUUID();

        // when
        Optional<EnvelopeSupplierStatement> statement = repo.findById(uuid);

        // then
        assertThat(statement).isEmpty();
    }

    @Test
    void should_return_empty_optional_if_statement_does_not_exist_for_given_day() {

        // when
        Optional<EnvelopeSupplierStatement> statement = repo.findLatest(LocalDate.now());

        // then
        assertThat(statement).isEmpty();
    }

    @Test
    void should_return_latest_statement_if_more_than_one_statement_exist_for_given_day()
        throws SQLException {
        // given
        LocalDate statementDate = LocalDate.now().minusDays(2);
        var newEnvelopeSupplierStatement =
            new NewEnvelopeSupplierStatement(
                statementDate,
                "{ \"b\": \"300\" }",
                "v1.0.0"
            );

        // when
        UUID id = repo.save(newEnvelopeSupplierStatement);
        assertThat(id).isNotNull();

        var latestAfterThisTime = LocalDateTime.now();
        // when
        UUID idLatest = repo.save(newEnvelopeSupplierStatement);

        // when
        Optional<EnvelopeSupplierStatement> statementOptional = repo.findLatest(statementDate);

        // then
        assertThat(statementOptional).isNotEmpty();
        var statement = statementOptional.get();
        assertThat(statement.createdAt).isAfter(latestAfterThisTime);
        assertThat(statement.id).isEqualTo(idLatest);
        assertThat(statement.date).isEqualTo(statementDate);
    }
}
