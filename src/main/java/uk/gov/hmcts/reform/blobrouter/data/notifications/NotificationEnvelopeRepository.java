package uk.gov.hmcts.reform.blobrouter.data.notifications;

import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public class NotificationEnvelopeRepository {

    private final NamedParameterJdbcTemplate jdbcTemplate;
    private final RejectedEnvelopeMapper mapper;

    public NotificationEnvelopeRepository(
        NamedParameterJdbcTemplate jdbcTemplate,
        RejectedEnvelopeMapper mapper
    ) {
        this.jdbcTemplate = jdbcTemplate;
        this.mapper = mapper;
    }

    public List<RejectedEnvelope> getRejectedEnvelopes() {
        return jdbcTemplate.query(
            "SELECT env.file_name, env.container, event.notes as errorDescription "
                + " FROM envelopes env, envelope_events event "
                + " WHERE env.id = event.envelope_id "
                + " AND event.type = 'REJECTED'",
            this.mapper
        );
    }
}
