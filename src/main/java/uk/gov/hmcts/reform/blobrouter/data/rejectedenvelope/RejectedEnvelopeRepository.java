package uk.gov.hmcts.reform.blobrouter.data.rejectedenvelope;

import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * The `RejectedEnvelopeRepository` class in Java uses a JDBC template to retrieve a list of rejected envelopes
 * along with their details from a database.
 */
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

    /**
     * The function `getRejectedEnvelopes` retrieves a list of rejected envelopes along with their details
     * from a database using a JDBC template in Java.
     *
     * @return A list of `RejectedEnvelope` objects containing the fields `id`, `file_name`, `container`,
     *      `error_code`, and `errorDescription` from the database query result. The query selects data from the
     *      `envelopes` and `envelope_events` tables where the `event.type` is 'REJECTED' and
     *      `env.pending_notification` is TRUE.
     */
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
