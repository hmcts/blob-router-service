package uk.gov.hmcts.reform.blobrouter.data.reports;

import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;
import uk.gov.hmcts.reform.blobrouter.model.out.reports.EnvelopeCountSummaryReportItem;

import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

@Repository
public class ReportRepository {

    private static final String EXCLUDED_CONTAINER = "bulkscan";

    private final NamedParameterJdbcTemplate jdbcTemplate;
    private final EnvelopeSummaryMapper mapper;
    private final EnvelopeCountSummaryMapper summaryMapper;

    public ReportRepository(
        NamedParameterJdbcTemplate jdbcTemplate,
        EnvelopeSummaryMapper mapper,
        EnvelopeCountSummaryMapper summaryMapper
    ) {
        this.jdbcTemplate = jdbcTemplate;
        this.mapper = mapper;
        this.summaryMapper = summaryMapper;
    }

    public List<EnvelopeSummary> getEnvelopeSummary(Instant from, Instant to) {
        return jdbcTemplate.query(
            "SELECT container, file_name, file_created_at, dispatched_at, status, is_deleted "
                + "FROM envelopes "
                + "WHERE container <> '" + EXCLUDED_CONTAINER + "' "
                + "  AND file_created_at >= :from "
                + "  AND file_created_at < :to "
                + "ORDER BY file_created_at",
            new MapSqlParameterSource()
                .addValue("from", Timestamp.from(from))
                .addValue("to", Timestamp.from(to)),
            this.mapper
        );
    }

    public List<EnvelopeCountSummaryReportItem> getReportFor(LocalDate date) {
        return jdbcTemplate.query(
            "SELECT\n"
                + "  container,\n"
                + "  date(created_at) AS date,\n"
                + "  count(*) AS received,\n"
                + "  SUM(CASE WHEN status = 'REJECTED' THEN 1 ELSE 0 END) AS rejected\n"
                + "FROM (\n"
                + "  Envelopes"
                + "GROUP BY container, date(created_at)\n"
                + "HAVING date(created_at) = :date\n",
            new MapSqlParameterSource()
                .addValue("date", Instant.from(date)),
            this.summaryMapper
        );
    }

}
