package uk.gov.hmcts.reform.blobrouter.data.events;

import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public class EnvelopeEventRepository {

    private final NamedParameterJdbcTemplate jdbcTemplate;
    private final EnvelopeEventMapper mapper;

    public EnvelopeEventRepository(NamedParameterJdbcTemplate jdbcTemplate, EnvelopeEventMapper mapper) {
        this.jdbcTemplate = jdbcTemplate;
        this.mapper = mapper;
    }

    public List<EnvelopeEvent> findForEnvelope(UUID envelopeId) {
        return jdbcTemplate.query(
            "SELECT * FROM envelope_events WHERE envelope_id = :envelopeId",
            new MapSqlParameterSource("envelopeId", envelopeId),
            this.mapper
        );
    }

    public List<EnvelopeEvent> findForEnvelopes(List<UUID> envelopeIds) {
        return jdbcTemplate.query(
            "SELECT * FROM envelope_events WHERE envelope_id in (:envelopeIds) ORDER BY id DESC",
            new MapSqlParameterSource("envelopeIds", envelopeIds),
            this.mapper
        );
    }

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
