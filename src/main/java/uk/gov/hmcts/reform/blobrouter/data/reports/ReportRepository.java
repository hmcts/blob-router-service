package uk.gov.hmcts.reform.blobrouter.data.reports;

import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;
import uk.gov.hmcts.reform.blobrouter.model.out.reports.EnvelopeCountSummaryReportItem;

import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.StringJoiner;

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

    public List<EnvelopeCountSummaryReportItem> getReportFor(LocalDate date, List<String> containersList) {
        StringJoiner containers = new StringJoiner(", ");
        containersList.forEach(containerName -> containers.add("('" + containerName + "')"));
        return jdbcTemplate.query(
                  "SELECT A.container, (CASE WHEN G.received IS NULL THEN 0 ELSE G.received END) AS received,"
                      + " (CASE WHEN G.rejected IS NULL THEN 0 ELSE G.rejected END) AS rejected,:date as date"
                      + "  FROM(VALUES" + containers + ") AS A (container)"
                      + "  LEFT JOIN"
                      + "  (SELECT container as Container, date (created_at) AS date,"
                      + "  count(*) as received,"
                      + "  SUM (CASE WHEN Envelopes.status = 'REJECTED' THEN 1 ELSE 0 END) AS rejected"
                      + "  FROM Envelopes"
                      + "  GROUP BY container, date(created_at)"
                      + "  HAVING date(created_at) = :date)"
                      + "  AS G on A.container = G.container",
            new MapSqlParameterSource()
                .addValue("date", date),
            this.summaryMapper
        );
    }

}
