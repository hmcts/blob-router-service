package uk.gov.hmcts.reform.blobrouter.data.reconciliation;

import org.postgresql.util.PGobject;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import javax.validation.ClockProvider;
import java.sql.SQLException;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

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

    public void save(NewEnvelopeSupplierStatement statement) throws SQLException {
        jdbcTemplate.update(
            "INSERT INTO envelope_supplier_statements"
                + "(id, date, content, content_type_version, created_at) "
                + "VALUES "
                + "(:id, :date, :content, :contentTypeVersion, :createdAt)",
            new MapSqlParameterSource()
                .addValue("id", statement.id)
                .addValue("date", statement.date)
                .addValue("content", toJson(statement.content))
                .addValue("contentTypeVersion", statement.contentTypeVersion)
                .addValue("createdAt", LocalDateTime.now(clock))
        );
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

    private PGobject toJson(String string) throws SQLException {
        var json = new PGobject();
        json.setType("json");
        json.setValue(string);
        return json;
    }

}
