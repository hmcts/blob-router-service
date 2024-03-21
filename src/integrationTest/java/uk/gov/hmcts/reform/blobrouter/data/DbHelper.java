package uk.gov.hmcts.reform.blobrouter.data;

import org.springframework.context.annotation.Profile;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.reform.blobrouter.data.envelopes.NewEnvelope;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.UUID;

/**
 * The `DbHelper` class in Java provides methods to delete all records from multiple tables and insert a new envelope
 * record with a specified creation timestamp using Spring's JdbcTemplate.
 */
@Profile("db-test")
@Component
public class DbHelper {

    private final NamedParameterJdbcTemplate jdbcTemplate;

    public DbHelper(NamedParameterJdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    /**
     * The `deleteAll` function deletes all records from multiple tables in a database using Spring's JdbcTemplate.
     */
    public void deleteAll() {
        jdbcTemplate.update("DELETE FROM envelope_events", new MapSqlParameterSource());
        jdbcTemplate.update("DELETE FROM envelopes", new MapSqlParameterSource());
        jdbcTemplate.update("DELETE FROM envelope_supplier_statements", new MapSqlParameterSource());
        jdbcTemplate.update("DELETE FROM envelope_reconciliation_reports", new MapSqlParameterSource());
    }

    /**
     * The `insertWithCreatedAt` function inserts a new envelope record into a database table with a specified creation
     * timestamp and returns the generated UUID identifier.
     *
     * @param envelope  The `insertWithCreatedAt` method you provided inserts a new envelope record into a database
     *                  table named `envelopes`. It generates a UUID for the envelope ID, sets the provided `createdAt`
     *                  timestamp, and inserts the envelope data into the table using Spring's `JdbcTemplate`.
     * @param createdAt The `createdAt` parameter in the `insertWithCreatedAt` method represents the
     *                  timestamp indicating when the envelope was created in the database.
     *                  This timestamp is used to populate the `created_at` column in the `envelopes` table when
     *                  inserting a new envelope record.
     * @return The method `insertWithCreatedAt` returns a `UUID` which is the unique identifier generated
     *      for the inserted envelope.
     */
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
