package uk.gov.hmcts.reform.blobrouter.data.events;

import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

/**
 * The `EnvelopeEventRepository` class in Java provides methods to interact with a database table
 * storing envelope events, including retrieving events by envelope ID, multiple envelope IDs,
 * and inserting new events.
 */
@Repository
public class EnvelopeEventRepository {

    private final NamedParameterJdbcTemplate jdbcTemplate;
    private final EnvelopeEventMapper mapper;

    public EnvelopeEventRepository(NamedParameterJdbcTemplate jdbcTemplate, EnvelopeEventMapper mapper) {
        this.jdbcTemplate = jdbcTemplate;
        this.mapper = mapper;
    }

    /**
     * The function `findForEnvelope` retrieves a list of `EnvelopeEvent` objects from the database based
     * on the provided `envelopeId`.
     *
     * @param envelopeId The `envelopeId` parameter is a UUID that is used to search for envelope events
     *      in the database. The `findForEnvelope` method executes a SQL query to retrieve all envelope events
     *      associated with the specified `envelopeId`.
     * @return A List of EnvelopeEvent objects that correspond to the given envelopeId.
     */
    public List<EnvelopeEvent> findForEnvelope(UUID envelopeId) {
        return jdbcTemplate.query(
            "SELECT * FROM envelope_events WHERE envelope_id = :envelopeId",
            new MapSqlParameterSource("envelopeId", envelopeId),
            this.mapper
        );
    }

    /**
     * This Java function retrieves envelope events for a list of envelope IDs from a database table.
     *
     * @param envelopeIds A list of UUIDs representing envelope IDs for which you want to find corresponding envelope
     *      events.
     * @return A list of `EnvelopeEvent` objects corresponding to the envelope IDs provided in the `envelopeIds` list.
     *      The query selects all records from the `envelope_events` table where the `envelope_id` is in the list of
     *      `envelopeIds`, and the results are ordered by the `id` in descending order.
     */
    public List<EnvelopeEvent> findForEnvelopes(List<UUID> envelopeIds) {
        return jdbcTemplate.query(
            "SELECT * FROM envelope_events WHERE envelope_id in (:envelopeIds) ORDER BY id DESC",
            new MapSqlParameterSource("envelopeIds", envelopeIds),
            this.mapper
        );
    }

    /**
     * The function inserts a new envelope event into a database table and returns the generated key.
     *
     * @param event The `insert` method you provided is used to insert a new record into the `envelope_events`
     *      table in a database using Spring's `JdbcTemplate`.
     * @return The method `insert` is returning the generated key value of the inserted record in the `envelope_events`
     *      table. This key value is retrieved using the `KeyHolder` object after the insertion operation is performed.
     */
    public long insert(NewEnvelopeEvent event) {
        KeyHolder keyHolder = new GeneratedKeyHolder();

        jdbcTemplate.update(
            "INSERT INTO envelope_events (envelope_id, type, error_code, notes, created_at) "
                + "VALUES (:envelopeId, :type, :errorCode, :notes, CURRENT_TIMESTAMP)",
            new MapSqlParameterSource()
                .addValue("envelopeId", event.envelopeId)
                .addValue("type", event.type.name())
                .addValue("errorCode", event.errorCode != null ? event.errorCode.name() : null)
                .addValue("notes", event.notes),
            keyHolder,
            new String[]{"id"}
        );

        return (long) keyHolder.getKey();
    }
}
