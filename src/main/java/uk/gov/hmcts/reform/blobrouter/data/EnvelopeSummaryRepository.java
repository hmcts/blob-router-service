package uk.gov.hmcts.reform.blobrouter.data;

import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;
import uk.gov.hmcts.reform.blobrouter.data.model.EnvelopeSummary;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;

@Repository
public class EnvelopeSummaryRepository {

    private static final String BULKSCAN_CONTAINER_NAME = "bulkscan";

    private final NamedParameterJdbcTemplate jdbcTemplate;
    private final EnvelopeSummaryMapper mapper;

    public EnvelopeSummaryRepository(
        NamedParameterJdbcTemplate jdbcTemplate,
        EnvelopeSummaryMapper mapper
    ) {
        this.jdbcTemplate = jdbcTemplate;
        this.mapper = mapper;
    }

    public List<EnvelopeSummary> find(Instant from, Instant to) {
        return jdbcTemplate.query(
              "SELECT e.container, e.file_name, e.file_created_at, e.dispatched_at AS file_dispatched_at, "
                + "       e.status, e.is_deleted, "
                + "       ev2.created_at AS event_created_at, ev2.event, ev2.notes "
                + "FROM envelopes e "
                + "LEFT OUTER JOIN ("
                + "    SELECT container, file_name, MAX(id) AS max_id "
                + "    FROM events "
                + "    GROUP BY container, file_name "
                + ") ev1 "
                + "ON (e.container = ev1.container AND e.file_name = ev1.file_name) "
                + "LEFT OUTER JOIN ("
                + "    SELECT id, event, notes, created_at "
                + "    FROM events"
                + ") ev2 "
                + "ON ev2.id = ev1.max_id "
                + "WHERE e.container <> '" + BULKSCAN_CONTAINER_NAME + "' "
                + "      AND e.file_created_at >= :from AND e.file_created_at < :to",
            new MapSqlParameterSource()
                .addValue("from", Timestamp.from(from))
                .addValue("to", Timestamp.from(to)),
            this.mapper
        );
    }
}
