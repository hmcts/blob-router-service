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
            "SELECT e.*, ev2.* "
                + "FROM envelopes e "
                + "LEFT OUTER JOIN ("
                + "    SELECT container, file_name, MAX(id) AS max_id "
                + "    FROM events "
                + "    GROUP BY container, file_name "
                + ") ev1 "
                + "ON (e.container = ev1.container AND e.file_name = ev1.file_name) "
                + "LEFT OUTER JOIN ("
                + "    SELECT * "
                + "    FROM events"
                + ") ev2 "
                + "ON ev2.id = ev1.max_id "
                + "WHERE e.file_created_at >= :from AND e.file_created_at < :to",
            new MapSqlParameterSource()
                .addValue("from", Timestamp.from(from))
                .addValue("to", Timestamp.from(to)),
            this.mapper
        );
    }
}
