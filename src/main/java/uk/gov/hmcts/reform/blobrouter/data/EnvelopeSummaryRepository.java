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
              "SELECT container, file_name, file_created_at, dispatched_at, "
                + "       status, is_deleted "
                + "FROM envelopes "
                + "WHERE container <> '" + BULKSCAN_CONTAINER_NAME + "' "
                + "      AND file_created_at >= :from AND file_created_at < :to "
                + "ORDER BY file_created_at",
            new MapSqlParameterSource()
                .addValue("from", Timestamp.from(from))
                .addValue("to", Timestamp.from(to)),
            this.mapper
        );
    }
}
