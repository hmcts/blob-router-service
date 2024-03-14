package uk.gov.hmcts.reform.blobrouter.data.reconciliation.statements;

import org.slf4j.Logger;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;
import uk.gov.hmcts.reform.blobrouter.data.reconciliation.statements.model.EnvelopeSupplierStatement;
import uk.gov.hmcts.reform.blobrouter.data.reconciliation.statements.model.NewEnvelopeSupplierStatement;

import java.sql.SQLException;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;
import javax.validation.ClockProvider;

import static org.slf4j.LoggerFactory.getLogger;
import static uk.gov.hmcts.reform.blobrouter.data.Utils.toJson;

/**
 * The `SupplierStatementRepository` class in Java provides methods to save and retrieve envelope supplier
 * statements from a database using jdbcTemplate.
 */
@Repository
public class SupplierStatementRepository {
    private static final Logger logger = getLogger(SupplierStatementRepository.class);

    private final NamedParameterJdbcTemplate jdbcTemplate;
    private final EnvelopeSupplierStatementRowMapper rowMapper;
    private final Clock clock;

    public SupplierStatementRepository(
        NamedParameterJdbcTemplate jdbcTemplate,
        EnvelopeSupplierStatementRowMapper rowMapper,
        ClockProvider clockProvider
    ) {
        this.jdbcTemplate = jdbcTemplate;
        this.rowMapper = rowMapper;
        this.clock = clockProvider.getClock();
    }

    /**
     * The `save` function inserts a new envelope supplier statement into a database table and returns
     * the generated UUID identifier.
     *
     * @param statement The `save` method you provided is used to save a `NewEnvelopeSupplierStatement` object into a
     *      database table named `envelope_supplier_statements`. The method generates a random UUID as the ID for
     *      the statement, inserts the statement data into the table, and then returns the generated UUID.
     * @return The method `save` is returning a `UUID` which represents the unique identifier of the saved
     *      `NewEnvelopeSupplierStatement`.
     */
    public UUID save(NewEnvelopeSupplierStatement statement) throws SQLException {
        UUID id = UUID.randomUUID();
        jdbcTemplate.update(
            "INSERT INTO envelope_supplier_statements"
                + "(id, date, content, content_type_version, created_at) "
                + "VALUES "
                + "(:id, :date, :content, :contentTypeVersion, :createdAt)",
            new MapSqlParameterSource()
                .addValue("id", id)
                .addValue("date", statement.date)
                .addValue("content", toJson(statement.content))
                .addValue("contentTypeVersion", statement.contentTypeVersion)
                .addValue("createdAt", LocalDateTime.now(clock))
        );
        logger.info("Supplier statement saved with id {}", id);
        return id;
    }

    /**
     * This Java function retrieves an EnvelopeSupplierStatement object by its UUID from a database using
     * jdbcTemplate and returns it wrapped in an Optional, handling EmptyResultDataAccessException by returning
     * an empty Optional.
     *
     * @param id The `id` parameter is a `UUID` type used to uniquely identify an `EnvelopeSupplierStatement` in the
     *      database. It is used in the SQL query to retrieve the specific `EnvelopeSupplierStatement` with the
     *      matching `id` from the `envelope_supplier_statements` table.
     * @return An Optional containing either the EnvelopeSupplierStatement with the specified ID if found, or an empty
     *      Optional if no result is found.
     */
    public Optional<EnvelopeSupplierStatement> findById(UUID id) {
        try {
            EnvelopeSupplierStatement statement = jdbcTemplate.queryForObject(
                "SELECT * FROM envelope_supplier_statements WHERE id = :id",
                new MapSqlParameterSource("id", id),
                this.rowMapper
            );
            return Optional.of(statement);
        } catch (EmptyResultDataAccessException ex) {
            return Optional.empty();
        }
    }

    /**
     * finds latest (by column `created_at`) created supplier statement by `date`.
     * @param date supplier statement date
     * @return latest supplier statement
     */
    public Optional<EnvelopeSupplierStatement> findLatest(LocalDate date) {
        try {
            EnvelopeSupplierStatement statement = jdbcTemplate.queryForObject(
                "SELECT * FROM envelope_supplier_statements WHERE date = :date "
                    + "ORDER BY created_at DESC "
                    + "LIMIT 1",
                new MapSqlParameterSource("date", date),
                this.rowMapper
            );
            return Optional.of(statement);
        } catch (EmptyResultDataAccessException ex) {
            return Optional.empty();
        }
    }
}
