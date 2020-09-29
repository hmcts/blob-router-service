package uk.gov.hmcts.reform.blobrouter.data.reconciliation.statements;

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

import static uk.gov.hmcts.reform.blobrouter.data.Utils.toJson;

@Repository
public class SupplierStatementRepository {

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
        return id;
    }

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
