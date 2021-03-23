package uk.gov.hmcts.reform.blobrouter.data;

import org.springframework.context.annotation.Profile;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.reform.blobrouter.data.envelopes.NewEnvelope;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.UUID;

@Profile("db-test")
@Component
public class DbHelper {

    private final NamedParameterJdbcTemplate jdbcTemplate;

    public DbHelper(NamedParameterJdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public void deleteAll() {
        jdbcTemplate.update("DELETE FROM envelope_events", new MapSqlParameterSource());
        jdbcTemplate.update("DELETE FROM envelopes", new MapSqlParameterSource());
        jdbcTemplate.update("DELETE FROM envelope_supplier_statements", new MapSqlParameterSource());
        jdbcTemplate.update("DELETE FROM envelope_reconciliation_reports", new MapSqlParameterSource());
    }

    public UUID insertWithCreatedAt(NewEnvelope envelope, Instant createdAt) {
        UUID id = UUID.randomUUID();
        jdbcTemplate.update(
            "INSERT INTO envelopes (id, container, file_name, file_created_at, status, dispatched_at, created_at) "
                + "VALUES (:id, :container, :fileName, :fileCreatedAt, :status, :dispatchedAt, :createdAt)",
            new MapSqlParameterSource()
                .addValue("id", id)
                .addValue("container", envelope.container)
                .addValue("fileName", envelope.fileName)
                .addValue("fileCreatedAt", Timestamp.from(envelope.fileCreatedAt))
                .addValue("status", envelope.status.name())
                .addValue(
                    "dispatchedAt",
                    envelope.dispatchedAt == null ? null : Timestamp.from(envelope.dispatchedAt)
                )
                .addValue("createdAt", Timestamp.from(createdAt))
        );
        return id;
    }
}
