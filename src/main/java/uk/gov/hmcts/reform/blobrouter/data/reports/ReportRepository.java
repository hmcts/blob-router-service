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
                + "  first_events.container AS container,\n"
                + "  date(first_events.createdat) AS date,\n"
                + "  count(*) AS received,\n"
                + "  SUM(CASE WHEN rejection_events.id IS NOT NULL THEN 1 ELSE 0 END) AS rejected\n"
                + "FROM (\n"
                + "  SELECT DISTINCT on (container, zipfilename)\n"
                + "    container, zipfilename, createdat\n"
                + "  FROM process_events\n"
                + "  ORDER BY container, zipfilename, createdat ASC\n"
                + ") AS first_events\n"
                + "LEFT JOIN (\n"
                + "  SELECT DISTINCT on (container, zipfilename)\n"
                + "    id, container, zipfilename\n"
                + "  FROM process_events\n"
                + "  WHERE event IN ('DOC_FAILURE', 'FILE_VALIDATION_FAILURE', 'DOC_SIGNATURE_FAILURE')\n"
                + ") AS rejection_events\n"
                + "ON rejection_events.container = first_events.container\n"
                + "AND rejection_events.zipfilename = first_events.zipfilename\n"
                + "GROUP BY first_events.container, date(first_events.createdat)\n"
                + "HAVING date(first_events.createdat) = :date\n",
            new MapSqlParameterSource()
                .addValue("date", Instant.from(date)),
            this.summaryMapper
        );
    }

}
