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
            "SELECT env.id, env.container, env.file_name, e.type, e.notes as errorDescription "
                + " FROM envelopes env "
                + " JOIN "
                + " envelope_events e ON e.envelope_id = env.id " /* get container and file_name from envelope */
                + " JOIN "
                + " (SELECT envelope_id, SUM(notification_event) AS notification_sent_count "
                + "  FROM "
                + "     (SELECT envelope_id, type, "
                + "      (CASE WHEN type = 'NOTIFICATION_SENT' THEN 1 else 0 END) AS notification_event "
                + "      FROM envelope_events "
                + "      WHERE type IN ('REJECTED', 'NOTIFICATION_SENT') "
                + "      GROUP BY envelope_id, type "
                + "     ) events " /* envelopes having Rejected and Notification_sent events */
                + "  GROUP BY envelope_id "
                + " ) rejected_envelopes " /* rejected envelope_ids from events */
                + " ON e.envelope_id = rejected_envelopes.envelope_id " /* join on events and rejected envelopes */
                + " WHERE e.type = 'REJECTED' "
                + "     AND rejected_envelopes.notification_sent_count = 0", /* filter notification_sent */
            this.mapper
        );
    }
}
