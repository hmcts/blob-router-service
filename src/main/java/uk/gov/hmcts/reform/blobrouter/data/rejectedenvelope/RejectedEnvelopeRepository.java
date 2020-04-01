package uk.gov.hmcts.reform.blobrouter.data.rejectedenvelope;

import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public class RejectedEnvelopeRepository {

    private final NamedParameterJdbcTemplate jdbcTemplate;
    private final RejectedEnvelopeMapper mapper;

    public RejectedEnvelopeRepository(
        NamedParameterJdbcTemplate jdbcTemplate,
        RejectedEnvelopeMapper mapper
    ) {
        this.jdbcTemplate = jdbcTemplate;
        this.mapper = mapper;
    }

    public List<RejectedEnvelope> getRejectedEnvelopes() {
        return jdbcTemplate.query(
            "SELECT env.id, env.container, env.file_name, e.notes as errorDescription "
                + " FROM envelopes env "
                + " JOIN envelope_events e "
                + "     ON e.envelope_id = env.id "
                + " JOIN "
                + "     (SELECT envelope_id, count(type) AS events_count "
                + "     FROM "
                + "         (SELECT envelope_id, type "
                + "         FROM envelope_events "
                + "         WHERE type IN ('REJECTED', 'NOTIFICATION_SENT') "
                + "     ) events " /* Rejected and Notification_sent events */
                + "     GROUP BY envelope_id "
                + " ) rejected_envelopes " /* envelope_ids and events_count which would be 2 if notification_sent */
                + "     ON e.envelope_id = rejected_envelopes.envelope_id "
                + " WHERE e.type = 'REJECTED' "
                + "     AND rejected_envelopes.events_count = 1", /* filter notification_sent events */
            this.mapper
        );
    }
}
