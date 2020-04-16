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
            "SELECT env.id, env.file_name, env.container, event.error_code, event.notes as errorDescription "
                + " FROM envelopes env, envelope_events event "
                + " WHERE env.id = event.envelope_id "
                + "     AND event.type = 'REJECTED' "
                + "     AND env.pending_notification IS TRUE ",
            this.mapper
        );
    }
}
